package api;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Member;
import service.MemberService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MemberHandler implements HttpHandler {
    private static final String PREFIX = "/api/members";
    private final MemberService memberService;
    public MemberHandler(MemberService memberService) {
        this.memberService = memberService;
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
            ApiServer.sendResponse(ex, 500, "{\"error\":\"" + ProductHandler.escapeJson(e.getMessage()) + "\"}");
        }
    }
    // ── GET
    private void handleGet(HttpExchange ex, String id) throws IOException {
        if (id == null) {
            List<Member> list = memberService.semuaMember();
            ApiServer.sendResponse(ex, 200, membersToJson(list));
        } else {
            Optional<Member> opt = memberService.cariById(id);
            if (opt.isPresent()) {
                ApiServer.sendResponse(ex, 200, memberToJson(opt.get()));
            } else {
                // Coba cari by phone juga (untuk keperluan transaksi)
                opt = memberService.cariByPhone(id);
                if (opt.isPresent()) {
                    ApiServer.sendResponse(ex, 200, memberToJson(opt.get()));
                } else {
                    ApiServer.sendResponse(ex, 404, "{\"error\":\"Member tidak ditemukan\"}");
                }
            }
        }
    }
    // ── POST
    private void handlePost(HttpExchange ex) throws IOException {
        String body = ApiServer.readBody(ex);
        try {
            String nama  = JsonParser.getString(body, "name");
            String phone = JsonParser.getString(body, "phone");
            String email = JsonParser.getString(body, "email");
            String tier  = JsonParser.getString(body, "tier");
            Member m = memberService.daftarMember(nama, phone, email, tier);
            ApiServer.sendResponse(ex, 201, memberToJson(m));
        } catch (IllegalArgumentException e) {
            // Nomor telepon sudah terdaftar, dll.
            ApiServer.sendResponse(ex, 409, "{\"error\":\"" + ProductHandler.escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"" + ProductHandler.escapeJson(e.getMessage()) + "\"}");
        }
    }
    // ── PUT
    private void handlePut(HttpExchange ex, String id) throws IOException {
        if (id == null) { ApiServer.sendResponse(ex, 400, "{\"error\":\"ID diperlukan\"}"); return; }
        String body = ApiServer.readBody(ex);
        try {
            String nama  = JsonParser.getString(body, "name");
            String phone = JsonParser.getString(body, "phone");
            String email = JsonParser.getString(body, "email");
            String tier = JsonParser.getString(body, "tier");
            boolean ok  = memberService.updateMember(id, nama, phone, email, tier);
            if (ok) {
                ApiServer.sendResponse(ex, 200, "{\"message\":\"Member diupdate\"}");
            } else {
                ApiServer.sendResponse(ex, 404, "{\"error\":\"Member tidak ditemukan\"}");
            }
        } catch (Exception e) {
            ApiServer.sendResponse(ex, 400, "{\"error\":\"" + ProductHandler.escapeJson(e.getMessage()) + "\"}");
        }
    }
    // ── DELETE
    private void handleDelete(HttpExchange ex, String id) throws IOException {
        if (id == null) { ApiServer.sendResponse(ex, 400, "{\"error\":\"ID diperlukan\"}"); return; }
        boolean ok = memberService.hapusMember(id);
        if (ok) {
            ApiServer.sendResponse(ex, 200, "{\"message\":\"Member dihapus\"}");
        } else {
            ApiServer.sendResponse(ex, 404, "{\"error\":\"Member tidak ditemukan\"}");
        }
    }
    // ── Serialisasi JSON manual
    private String membersToJson(List<Member> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(memberToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    static String memberToJson(Member m) {
        return String.format("""
            {
              "id": "%s",
              "name": "%s",
              "phone": "%s",
              "email": "%s",
              "tier": "%s",
              "totalTransaksi": %d,
              "totalBelanja": %.0f
            }""",
                m.getMemberId(),
                ProductHandler.escapeJson(m.getName()),
                ProductHandler.escapeJson(m.getPhone()),
                ProductHandler.escapeJson(m.getEmail()),
                m.getTier().name(),
                m.getTotalTransaksi(),
                m.getTotalBelanja()
        );
    }
}