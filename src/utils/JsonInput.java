package utils;

/** Bound parser stack use while retaining brackets/escapes inside JSON strings. */
public final class JsonInput {
    public static final int MAX_DEPTH = 64;
    private JsonInput() {}
    public static boolean hasExcessiveDepth(String text) {
        int depth = 0;
        boolean quoted = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') {
                if (++depth > MAX_DEPTH) return true;
            } else if (c == '}' || c == ']') depth--;
        }
        return false;
    }
}
