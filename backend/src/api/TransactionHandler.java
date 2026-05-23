package api;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Member;
import models.Transaction;
import models.TransactionItem;
import service.MemberService;
import service.TransactionService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TransactionHandler implements HttpHandler {
    private static final String PREFIX = "/api/transactions";
    private final TransactionService transactionService;
    private final MemberService memberService;
    public TransactionHandler(TransactionService ts, MemberService ms) {
        this.transactionService = ts;
        this.memberService = ms;
    }
    @Override
    public void handle(HttpExchange ex) throws IOException {
        ApiServer.addCorsHeaders(ex);
        if (ApiServer.handlePreflight(ex)) {
            return;
        }
        String method = ex.getRequestMethod().toUpperCase();
        String id = ApiServer.extractId(ex, PREFIX);
        try {
            switch (method) {
                case "GET" -> handleGet(ex, id);
                case "POST" -> handlePost(ex);
                default ->
                        ApiServer.sendResponse(
                                ex,
                                405,
                                "{\"error\":\"Method not allowed\"}"
                        );
            }
        } catch (Exception e) {
            ApiServer.sendResponse(
                    ex,
                    500,
                    "{\"error\":\"" +
                            ProductHandler.escapeJson(e.getMessage()) +
                            "\"}"
            );
        }
    }
    // =========================================================
    // GET
    // =========================================================
    private void handleGet(HttpExchange ex, String id) throws IOException {
        if (id == null) {
            List<Transaction> list = transactionService.semuaTransaksi();
            ApiServer.sendResponse(
                    ex,
                    200,
                    transactionsToJson(list)
            );
        } else {
            Optional<Transaction> opt =
                    transactionService.cariById(id);
            if (opt.isPresent()) {
                ApiServer.sendResponse(
                        ex,
                        200,
                        transactionToJson(opt.get())
                );
            } else {
                ApiServer.sendResponse(
                        ex,
                        404,
                        "{\"error\":\"Transaksi tidak ditemukan\"}"
                );
            }
        }
    }
    // =========================================================
    // POST
    // =========================================================
    private void handlePost(HttpExchange ex) throws IOException {
        String body = ApiServer.readBody(ex);
        try {
            String memberPhone =
                    JsonParser.getStringOrNull(body, "memberPhone");
            Member member = null;
            if (memberPhone != null && !memberPhone.isBlank()) {
                member = memberService
                        .cariByPhone(memberPhone)
                        .orElse(null);
            }
            // Buat transaksi
            Transaction transaksi =
                    transactionService.buatTransaksi(member);
            // Parse items
            String itemsJson =
                    JsonParser.getArray(body, "items");
            String[] itemEntries =
                    splitJsonArray(itemsJson);
            StringBuilder errors = new StringBuilder();
            for (String itemJson : itemEntries) {
                String productId =
                        JsonParser.getString(itemJson, "productId");
                int qty =
                        JsonParser.getInt(itemJson, "qty");
                String err =
                        transactionService.tambahItem(
                                transaksi,
                                productId,
                                qty
                        );
                if (err != null) {
                    errors.append(err).append("; ");
                }
            }
            // Tidak ada item valid
            if (transaksi.getItems().isEmpty()) {

                ApiServer.sendResponse(
                        ex,
                        400,
                        "{\"error\":\"Tidak ada item valid: "
                                + ProductHandler.escapeJson(errors.toString())
                                + "\"}"
                );

                return;
            }
            // Simpan transaksi
            transactionService.simpanTransaksi(transaksi);
            // Catat transaksi member
            if (member != null) {
                memberService.catatTransaksi(
                        member,
                        transaksi.getTotalAfterDiscount()
                );
            }
            ApiServer.sendResponse(
                    ex,
                    201,
                    transactionToJson(transaksi)
            );
        } catch (Exception e) {
            ApiServer.sendResponse(
                    ex,
                    400,
                    "{\"error\":\""
                            + ProductHandler.escapeJson(e.getMessage())
                            + "\"}"
            );
        }
    }
    // =========================================================
    // JSON SERIALIZATION
    // =========================================================
    private String transactionsToJson(List<Transaction> list) {

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < list.size(); i++) {

            sb.append(transactionToJson(list.get(i)));

            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    private String transactionToJson(Transaction t) {
        StringBuilder items = new StringBuilder("[");
        List<TransactionItem> itemList = t.getItems();
        for (int i = 0; i < itemList.size(); i++) {
            TransactionItem item = itemList.get(i);
            items.append(String.format("""
            {
              "productId": "%s",
              "productName": "%s",
              "qty": %d,
              "pricePerItem": %.0f,
              "subtotal": %.0f
            }""",
                    item.getProductId(),
                    ProductHandler.escapeJson(item.getProductName()),
                    item.getQty(),
                    item.getPriceAtTime(),
                    item.getSubtotal()
            ));
            if (i < itemList.size() - 1) items.append(",");
        }
        items.append("]");
        String memberJson = t.getMember() == null
                ? "null"
                : MemberHandler.memberToJson(t.getMember());
        return String.format("""
        {
          "id": "%s",
          "date": "%s",
          "member": %s,
          "items": %s,
          "subtotal": %.0f,
          "memberDiscount": %.0f,
          "total": %.0f
        }""",
                t.getTransactionId(),
                ProductHandler.escapeJson(t.getFormattedDate()),
                memberJson,
                items.toString(),
                t.getTotalBeforeDiscount(),
                t.getTotalSavings(),
                t.getTotalAfterDiscount()
        );
    }
    // =========================================================
    // SPLIT JSON ARRAY
    // =========================================================
    private String[] splitJsonArray(String arrayJson) {
        if (arrayJson == null
                || arrayJson.isBlank()
                || arrayJson.equals("[]")) {
            return new String[0];
        }
        String inner = arrayJson.trim();
        // Hapus
        if (inner.startsWith("[")) {
            inner = inner.substring(1);
        }
        // Hapus ]
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        inner = inner.trim();

        if (inner.isEmpty()) {
            return new String[0];
        }
        // Split berdasarkan },{
        String[] parts =
                inner.split("\\},\\s*\\{");
        // Kembalikan { dan }
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].startsWith("{")) {
                parts[i] = "{" + parts[i];
            }

            if (!parts[i].endsWith("}")) {
                parts[i] = parts[i] + "}";
            }
        }
        return parts;
    }
}