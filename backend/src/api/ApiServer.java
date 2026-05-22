package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import service.MemberService;
import service.ProductService;
import service.TransactionService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * ApiServer — HTTP server berbasis com.sun.net.httpserver (built-in Java, no extra lib).
 * Mendaftarkan semua route dan mendelegasikan ke handler masing-masing.
 */
public class ApiServer {

    private static final int PORT = 8080;

    private final HttpServer server;

    public ApiServer(ProductService ps, MemberService ms, TransactionService ts) throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Daftarkan handler untuk setiap resource
        server.createContext("/api/products",     new ProductHandler(ps));
        server.createContext("/api/members",      new MemberHandler(ms));
        server.createContext("/api/transactions", new TransactionHandler(ts, ms));
    }

    public void start() {
        server.start();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  McLaren API Server — port " + PORT + "      ║");
        System.out.println("║  http://localhost:" + PORT + "/api/products  ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Server berhenti.");
    }

    // ── Utility statis untuk dipakai semua handler ──────────────────

    /** Tambahkan CORS + Content-Type header ke setiap response */
    static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    /** Kirim response JSON dengan status code tertentu */
    static void sendResponse(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Baca request body sebagai String */
    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Ekstrak segment path setelah prefix.
     * Contoh: /api/products/PRD-0001 → "PRD-0001"
     * Kalau tidak ada ID → null
     */
    static String extractId(HttpExchange ex, String prefix) {
        String path = ex.getRequestURI().getPath();
        String after = path.substring(prefix.length());
        if (after.startsWith("/")) after = after.substring(1);
        return after.isBlank() ? null : after;
    }

    /** Handle OPTIONS preflight (CORS) */
    static boolean handlePreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}