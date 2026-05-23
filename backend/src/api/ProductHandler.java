package api;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Product;
import service.ProductService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ProductHandler implements HttpHandler {
    private static final String PREFIX = "/api/products";
    private final ProductService productService;
    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }
    @Override
    public void handle(HttpExchange ex) throws IOException {
        ApiServer.addCorsHeaders(ex);
        if (ApiServer.handlePreflight(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String id     = ApiServer.extractId(ex, PREFIX);
        try {
            switch (method) {
                case "GET"    -> handleGet(ex, id);
                case "POST"   -> handlePost(ex);
                case "PUT"    -> handlePut(ex, id);
                case "DELETE" -> handleDelete(ex, id);
                default       -> ApiServer.sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            ApiServer.sendResponse(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    // ── GET
    private void handleGet(HttpExchange ex, String id) throws IOException {
        if (id == null) {
            // GET /api/products → semua produk
            List<Product> list = productService.semuaProduk();
            ApiServer.sendResponse(ex, 200, productsToJson(list));
        } else {
            // GET /api/products/{id}
            Optional<Product> opt = productService.cariById(id);
            if (opt.isPresent()) {
                ApiServer.sendResponse(ex, 200, productToJson(opt.get()));
            } else {
                ApiServer.sendResponse(ex, 404, "{\"error\":\"Produk tidak ditemukan\"}");
            }
        }
    }

    // ── POST
    private void handlePost(HttpExchange ex) throws IOException {
        String body = ApiServer.readBody(ex);
        String name     = JsonParser.getString(body, "name");
        String category = JsonParser.getString(body, "category");
        String jenis    = JsonParser.getString(body, "jenis");
        String size     = JsonParser.getString(body, "size");
        // 1. Validasi Field
        if (name.isBlank() || category.isBlank() || jenis.isBlank()) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"Field 'name', 'category', dan 'jenis' wajib diisi!\"}");
            return;
        }
        try {
            double price   = JsonParser.getDouble(body, "price");
            int    stock   = JsonParser.getInt(body, "stock");
            // 2. Validasi Logika Angka
            if (price <= 0) {
                ApiServer.sendResponse(ex, 400, "{\"error\":\"Harga produk harus lebih dari 0!\"}");
                return;
            }
            if (stock < 0) {
                ApiServer.sendResponse(ex, 400, "{\"error\":\"Stok produk tidak boleh negatif!\"}");
                return;
            }
            boolean hasDsc = JsonParser.getBoolean(body, "hasDiscount");
            double  dscPct = hasDsc ? JsonParser.getDouble(body, "discountPercent") : 0.0;
            // 3. Validasi Diskon
            if (hasDsc && (dscPct <= 0 || dscPct > 100)) {
                ApiServer.sendResponse(ex, 400, "{\"error\":\"Persentase diskon harus antara 1% hingga 100%!\"}");
                return;
            }
            int jenisIndex = resolveJenisIndex(category, jenis);
            Product p = productService.tambahProduk(
                    name, price, stock, size, hasDsc, dscPct, category, jenisIndex
            );
            ApiServer.sendResponse(ex, 201, productToJson(p));
        } catch (IllegalArgumentException e) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            ApiServer.sendResponse(ex, 500, "{\"error\":\"Terjadi kesalahan format data angka\"}");
        }
    }
    // ── PUT
    private void handlePut(HttpExchange ex, String id) throws IOException {
        if (id == null) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"ID diperlukan\"}");
            return;
        }
        String body = ApiServer.readBody(ex);
        String name = JsonParser.getString(body, "name");
        if (name.isBlank()) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"Nama produk tidak boleh kosong!\"}");
            return;
        }
        try {
            double price  = JsonParser.getDouble(body, "price");
            int    stock  = JsonParser.getInt(body, "stock");
            String size   = JsonParser.getString(body, "size");
            boolean hasDsc= JsonParser.getBoolean(body, "hasDiscount");
            double dscPct = hasDsc ? JsonParser.getDouble(body, "discountPercent") : 0.0;
            if (price <= 0 || stock < 0 || (hasDsc && (dscPct <= 0 || dscPct > 100))) {
                ApiServer.sendResponse(ex, 400, "{\"error\":\"Data angka (harga/stok/diskon) tidak valid!\"}");
                return;
            }
            Optional<Product> opt = productService.cariById(id);
            if (opt.isEmpty()) {
                ApiServer.sendResponse(ex, 404, "{\"error\":\"Produk tidak ditemukan\"}");
                return;
            }
            Product p = opt.get();
            p.setName(name);
            p.setPrice(price);
            p.setStock(stock);
            p.setSize(size);
            p.setHasDiscount(hasDsc);
            p.setDiscountPercent(dscPct);

            boolean ok = productService.updateProdukUtuh(p);
            if (ok) {
                ApiServer.sendResponse(ex, 200, productToJson(p));
            } else {
                ApiServer.sendResponse(ex, 500, "{\"error\":\"Gagal update produk\"}");
            }
        } catch (Exception e) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"Format request tidak valid\"}");
        }
    }

    // ── DELETE
    private void handleDelete(HttpExchange ex, String id) throws IOException {
        if (id == null) { ApiServer.sendResponse(ex, 400, "{\"error\":\"ID diperlukan\"}"); return; }
        boolean ok = productService.hapusProduk(id);
        if (ok) {
            ApiServer.sendResponse(ex, 200, "{\"message\":\"Produk dihapus\"}");
        } else {
            ApiServer.sendResponse(ex, 404, "{\"error\":\"Produk tidak ditemukan\"}");
        }
    }
    // ── Serialisasi JSON manual
    private String productsToJson(List<Product> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(productToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    static String productToJson(Product p) {
        return String.format("""
            {
              "id": "%s",
              "name": "%s",
              "price": %.0f,
              "finalPrice": %.0f,
              "stock": %d,
              "size": "%s",
              "category": "%s",
              "jenis": "%s",
              "hasDiscount": %b,
              "discountPercent": %.0f,
              "statusStok": "%s"
            }""",
                p.getId(),
                escapeJson(p.getName()),
                p.getPrice(),
                p.getFinalPrice(),
                p.getStock(),
                escapeJson(p.getSize()),
                escapeJson(p.getCategory()),
                escapeJson(p.getJenis()),
                p.isHasDiscount(),
                p.getDiscountPercent(),
                p.getStatusStok()
        );
    }
    // ── Helper: cari index enum berdasarkan label
    private int resolveJenisIndex(String kategori, String jenisLabel) {
        String[][] JENIS_MAP = {
                {"Polo Shirt","Crew Neck","V-Neck","Oversized","Racing Tee"},
                {"Snapback Cap","Fitted Cap","Bucket Hat","Beanie","Visor"},
                {"Jacket","Hoodie","Windbreaker","Rain Jacket","Varsity Jacket"},
                {"Sneakers","Sandals","Boots","Slip-On","Racing Shoes"},
                {"Keychain","Mug","Phone Case","Lanyard","Sticker Pack","Model Car","Backpack","Wallet"}
        };
        String[] KATEGORI_LIST = {"T-Shirts","Headwear","Outerwear","Footwear","Gift & Accessories"};
        int katIdx = -1;
        for (int i = 0; i < KATEGORI_LIST.length; i++) {
            if (KATEGORI_LIST[i].equalsIgnoreCase(kategori)) { katIdx = i; break; }
        }
        if (katIdx == -1) throw new IllegalArgumentException("Kategori tidak valid: " + kategori);
        for (int j = 0; j < JENIS_MAP[katIdx].length; j++) {
            if (JENIS_MAP[katIdx][j].equalsIgnoreCase(jenisLabel)) return j;
        }
        throw new IllegalArgumentException("Jenis tidak valid: " + jenisLabel);
    }
    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}