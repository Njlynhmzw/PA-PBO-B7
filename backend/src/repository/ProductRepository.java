package repository;

import db.DatabaseConfig;
import models.*;

import java.sql.*;
import java.util.*;

public class ProductRepository {

    private Connection conn() { return DatabaseConfig.getConnection(); }

    private String generateId() {
        String updateSql = "UPDATE counters SET value = value + 1 WHERE name = 'product'";
        String selectSql = "SELECT value FROM counters WHERE name = 'product'";

        // Membungkus Connection, Statement, dan ResultSet dalam try-with-resources
        try (Connection conn = conn()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psSelect = conn.prepareStatement(selectSql)) {

                psUpdate.executeUpdate();
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        int num = rs.getInt(1);
                        conn.commit();
                        return String.format("PRD-%04d", num);
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal generate product ID", e);
        }
        return null;
    }

    public Product createNew(String nama, double harga, int stok, String size,
                             boolean hasDiscount, double discountPercent,
                             String kategori, int jenisIndex) {
        String newId = generateId();
        Product p = buildProduct(newId, nama, harga, stok, size,
                hasDiscount, discountPercent, kategori, jenisIndex);
        save(p);
        return p;
    }

    public void save(Product product) {
        String sql = """
            INSERT INTO products
                (id, name, price, stock, size, category, jenis, has_discount, discount_percent)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                name=VALUES(name), price=VALUES(price), stock=VALUES(stock),
                size=VALUES(size), has_discount=VALUES(has_discount),
                discount_percent=VALUES(discount_percent)
            """;

        // Gunakan try-with-resources agar koneksi otomatis ditutup
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString (1, product.getId());
            ps.setString (2, product.getName());
            ps.setDouble (3, product.getPrice());
            ps.setInt    (4, product.getStock());
            ps.setString (5, product.getSize());
            ps.setString (6, product.getCategory());
            ps.setString (7, product.getJenis());
            ps.setBoolean(8, product.isHasDiscount());
            ps.setDouble (9, product.getDiscountPercent());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menyimpan produk: " + e.getMessage(), e);
        }
    }

    public void update(Product product) {
        String sql = """
            UPDATE products
            SET name             = ?,
                price            = ?,
                stock            = ?,
                size             = ?,
                has_discount     = ?,
                discount_percent = ?
            WHERE id = ?
            """;

        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString (1, product.getName());
            ps.setDouble (2, product.getPrice());
            ps.setInt    (3, product.getStock());
            ps.setString (4, product.getSize());
            ps.setBoolean(5, product.isHasDiscount());
            ps.setDouble (6, product.getDiscountPercent());
            ps.setString (7, product.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengupdate produk: " + e.getMessage(), e);
        }
    }

    public Optional<Product> findById(String id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari produk: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY created_at ASC";
        List<Product> result = new ArrayList<>();

        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {

            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil produk: " + e.getMessage(), e);
        }
        return result;
    }

    public List<Product> findByCategory(String category) {
        String sql = "SELECT * FROM products WHERE category = ? ORDER BY created_at ASC";
        List<Product> result = new ArrayList<>();

        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari produk by kategori: " + e.getMessage(), e);
        }
        return result;
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus produk: " + e.getMessage(), e);
        }
    }

    public boolean existsById(String id) { return findById(id).isPresent(); }

    public int count() {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }

    public boolean isEmpty() { return count() == 0; }

    public Set<String> getAllIds() {
        Set<String> ids = new LinkedHashSet<>();
        String sql = "SELECT id FROM products ORDER BY created_at";

        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) ids.add(rs.getString(1));
        } catch (SQLException ignored) {}
        return ids;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        String  id      = rs.getString("id");
        String  name    = rs.getString("name");
        double  price   = rs.getDouble("price");
        int     stock   = rs.getInt("stock");
        String  size    = rs.getString("size");
        String  category= rs.getString("category");
        String  jenis   = rs.getString("jenis");
        boolean hasDsc  = rs.getBoolean("has_discount");
        double  dscPct  = rs.getDouble("discount_percent");

        int jenisIndex = resolveJenisIndex(category, jenis);
        return buildProduct(id, name, price, stock, size, hasDsc, dscPct, category, jenisIndex);
    }

    private Product buildProduct(String id, String nama, double harga, int stok, String size,
                                 boolean hasDiscount, double discountPercent,
                                 String kategori, int jenisIndex) {
        return switch (kategori) {
            case "T-Shirts" -> {
                TShirt p = new TShirt(nama, harga, stok, size, hasDiscount, discountPercent,
                        TShirt.JenisTShirt.values()[jenisIndex]);
                p.setId(id); yield p;
            }
            case "Headwear" -> {
                Headwear p = new Headwear(nama, harga, stok, size, hasDiscount, discountPercent,
                        Headwear.JenisHeadwear.values()[jenisIndex]);
                p.setId(id); yield p;
            }
            case "Outerwear" -> {
                Outerwear p = new Outerwear(nama, harga, stok, size, hasDiscount, discountPercent,
                        Outerwear.JenisOuterwear.values()[jenisIndex]);
                p.setId(id); yield p;
            }
            case "Footwear" -> {
                Footwear p = new Footwear(nama, harga, stok, size, hasDiscount, discountPercent,
                        Footwear.JenisFootwear.values()[jenisIndex]);
                p.setId(id); yield p;
            }
            case "Gift & Accessories" -> {
                GiftAccessory p = new GiftAccessory(nama, harga, stok, size, hasDiscount, discountPercent,
                        GiftAccessory.JenisGift.values()[jenisIndex]);
                p.setId(id); yield p;
            }
            default -> throw new IllegalArgumentException("Kategori tidak valid: " + kategori);
        };
    }

    // Menggunakan referensi ke ProductCategory yang terpusat
    private int resolveJenisIndex(String kategori, String jenisLabel) {
        int katIdx = -1;
        for (int i = 0; i < ProductCategory.KATEGORI_LIST.length; i++) {
            if (ProductCategory.KATEGORI_LIST[i].equalsIgnoreCase(kategori)) { katIdx = i; break; }
        }
        if (katIdx == -1) throw new IllegalArgumentException("Kategori tidak valid: " + kategori);

        for (int j = 0; j < ProductCategory.JENIS_MAP[katIdx].length; j++) {
            if (ProductCategory.JENIS_MAP[katIdx][j].equalsIgnoreCase(jenisLabel)) return j;
        }
        return 0; // fallback jika label salah
    }
}