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

public class ApiServer {
    private static final int PORT = 8080;
    private final HttpServer server;
    public ApiServer(ProductService ps, MemberService ms, TransactionService ts) throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());
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
    // ── Utility statis untuk dipakai semua handler
    static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    static void sendResponse(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
    static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String extractId(HttpExchange ex, String prefix) {
        String path = ex.getRequestURI().getPath();
        String after = path.substring(prefix.length());
        if (after.startsWith("/")) after = after.substring(1);
        return after.isBlank() ? null : after;
    }

    static boolean handlePreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}