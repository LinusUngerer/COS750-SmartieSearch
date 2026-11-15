package cos212.assess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader and writer, enough for the progress file. It handles objects,
 * arrays, strings, integers, booleans and null, so the project needs no JSON library.
 * Parsed objects are {@code Map<String, Object>}, arrays are {@code List<Object>} and
 * whole numbers are {@code Long}.
 */
final class Json {

    private static final int MAX_DEPTH = 64;

    private Json() {
    }

    // ---- writing -------------------------------------------------------------

    /** @return the value as indented JSON text ending in a newline */
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.append('\n').toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            quote(s, out);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = it.next();
                indent(out, depth + 1);
                quote(String.valueOf(entry.getKey()), out);
                out.append(": ");
                write(entry.getValue(), out, depth + 1);
                out.append(it.hasNext() ? ",\n" : "\n");
            }
            indent(out, depth);
            out.append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(out, depth + 1);
                write(list.get(i), out, depth + 1);
                out.append(i < list.size() - 1 ? ",\n" : "\n");
            }
            indent(out, depth);
            out.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
        }
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // ---- reading -------------------------------------------------------------

    /**
     * @param text JSON text
     * @return a {@code Map}, {@code List}, {@code String}, {@code Long}, {@code Double},
     *         {@code Boolean} or {@code null}
     * @throws IllegalArgumentException if the text is not valid JSON
     */
    static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("unexpected text after the value");
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException("Invalid JSON at position " + pos + ": " + message);
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("nested too deeply");
            }
            skipWhitespace();
            if (pos >= s.length()) {
                throw error("unexpected end of text");
            }
            char c = s.charAt(pos);
            if (c == '{') {
                return object(depth);
            } else if (c == '[') {
                return array(depth);
            } else if (c == '"') {
                return string();
            } else if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            } else if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            return number();
        }

        private Map<String, Object> object(int depth) {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != '"') {
                    throw error("expected a string key");
                }
                String key = string();
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != ':') {
                    throw error("expected ':'");
                }
                pos++;
                map.put(key, value(depth + 1));
                skipWhitespace();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                } else if (pos < s.length() && s.charAt(pos) == '}') {
                    pos++;
                    return map;
                } else {
                    throw error("expected ',' or '}'");
                }
            }
        }

        private List<Object> array(int depth) {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value(depth + 1));
                skipWhitespace();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                } else if (pos < s.length() && s.charAt(pos) == ']') {
                    pos++;
                    return list;
                } else {
                    throw error("expected ',' or ']'");
                }
            }
        }

        private String string() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw error("unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c < 0x20) {
                    throw error("control character in string");
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw error("unterminated escape");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> out.append(e);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("short unicode escape");
                        }
                        try {
                            out.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("bad unicode escape");
                        }
                        pos += 4;
                    }
                    default -> throw error("unknown escape");
                }
            }
        }

        private Object number() {
            int start = pos;
            while (pos < s.length() && "+-.eE0123456789".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            String text = s.substring(start, pos);
            if (text.isEmpty()) {
                throw error("unexpected character '" + s.charAt(pos) + "'");
            }
            try {
                if (text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
                    return Double.valueOf(text);
                }
                return Long.valueOf(text);
            } catch (NumberFormatException e) {
                pos = start;
                throw error("bad number");
            }
        }
    }

    // ---- typed access used when restoring state ------------------------------

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException(what + " must be a JSON object");
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?>) {
            return (List<Object>) value;
        }
        throw new IllegalArgumentException(key + " must be a JSON array");
    }

    /** @return the non-negative whole number at {@code key}, or 0 when it is absent */
    static int count(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Long n && n >= 0 && n <= Integer.MAX_VALUE) {
            return n.intValue();
        }
        throw new IllegalArgumentException(key + " must be a non-negative whole number");
    }

    /** @return the boolean at {@code key}, or false when it is absent */
    static boolean flag(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }
}
