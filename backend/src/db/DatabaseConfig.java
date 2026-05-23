package db;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {
    private static final Map<String, String> env = loadEnv();
    private static final String URL =
            env.get("DB_URL");
    private static final String USER =
            env.get("DB_USER");
    private static final String PASSWORD =
            env.get("DB_PASSWORD");
    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br =
                     new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    map.put(
                            parts[0].trim(),
                            parts[1].trim()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Gagal membaca file .env",
                    e
            );
        }
        return map;
    }
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            return DriverManager.getConnection(
                    System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/mclaren_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root",
                    System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : ""
            );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver MySQL tidak ditemukan! Pastikan file JAR sudah di-Add as Library.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Gagal koneksi database", e);
        }
    }
}