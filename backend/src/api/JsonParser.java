package api;

/**
 * JsonParser — parser JSON sederhana tanpa library eksternal.
 * Mendukung tipe: String, int, double, boolean, nested object, array.
 *
 * CATATAN: Parser ini cukup untuk kebutuhan proyek ini.
 * Untuk produksi sebenarnya, gunakan Gson atau Jackson.
 */
public class JsonParser {

    /**
     * Ambil nilai String dari JSON.
     * Contoh: getString({"name":"McLaren"}, "name") → "McLaren"
     */
    public static String getString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return "";

        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx == -1) return "";

        // Skip whitespace
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        if (json.charAt(start) == '"') {
            // String value
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(start + 1, end)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n");
        }
        return "";
    }

    /**
     * Sama seperti getString tapi kembalikan null jika key tidak ada atau value "".
     */
    public static String getStringOrNull(String json, String key) {
        String val = getString(json, key);
        return val.isBlank() ? null : val;
    }

    /**
     * Ambil nilai int dari JSON.
     */
    public static int getInt(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null || raw.isBlank()) return 0;
        try { return (int) Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Ambil nilai double dari JSON.
     */
    public static double getDouble(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null || raw.isBlank()) return 0.0;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    /**
     * Ambil nilai boolean dari JSON.
     */
    public static boolean getBoolean(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null) return false;
        return raw.trim().equalsIgnoreCase("true");
    }

    /**
     * Ambil nilai JSON array sebagai string mentah.
     * Contoh: getArray({"items":[...]}, "items") → "[...]"
     */
    public static String getArray(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return "[]";

        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx == -1) return "[]";

        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        if (start >= json.length() || json.charAt(start) != '[') return "[]";

        // Cari penutup bracket dengan menghitung kedalaman
        int depth = 0;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { end++; break; } }
            end++;
        }
        return json.substring(start, end);
    }

    // ── Internal helper ───────────────────────────────────────────────

    /**
     * Ambil raw value (tanpa tanda petik) untuk key non-string (number, boolean, null).
     */
    private static String getRawValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return null;

        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx == -1) return null;

        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        // Kalau string value, skip
        if (start < json.length() && json.charAt(start) == '"') return null;

        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            end++;
        }
        return json.substring(start, end);
    }
}