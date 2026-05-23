package api;

public class JsonParser {
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

    public static String getStringOrNull(String json, String key) {
        String val = getString(json, key);
        return val.isBlank() ? null : val;
    }
    public static int getInt(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null || raw.isBlank()) return 0;
        try { return (int) Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
    public static double getDouble(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null || raw.isBlank()) return 0.0;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
    public static boolean getBoolean(String json, String key) {
        String raw = getRawValue(json, key);
        if (raw == null) return false;
        return raw.trim().equalsIgnoreCase("true");
    }
    public static String getArray(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return "[]";

        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx == -1) return "[]";

        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

        if (start >= json.length() || json.charAt(start) != '[') return "[]";

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
    // ── Internal helper
    private static String getRawValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx == -1) return null;
        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx == -1) return null;
        int start = colonIdx + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
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