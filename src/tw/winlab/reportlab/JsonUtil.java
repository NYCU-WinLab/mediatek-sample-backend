package tw.winlab.reportlab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer for this service's request and response bodies.
 * No external dependency — just enough object/array/string/number/boolean/null
 * support for the payloads this API sends and receives.
 */
public final class JsonUtil {
    private JsonUtil() {}

    // ---------------------------------------------------------------- write

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Boolean b) {
            sb.append(b.toString());
        } else if (value instanceof Number n) {
            sb.append(n.toString());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeValue(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                writeValue(o, sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ---------------------------------------------------------------- read

    /** Parses a JSON object body into a Map. Returns an empty map for blank/invalid input,
     * so callers can treat "no usable body" uniformly as missing fields. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            Parser p = new Parser(raw);
            Object v = p.parseValue();
            if (v instanceof Map) return (Map<String, Object>) v;
        } catch (Exception ignored) {
            // fall through to empty map
        }
        return new LinkedHashMap<>();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
            this.i = 0;
        }

        Object parseValue() {
            skipWs();
            char c = peek();
            return switch (c) {
                case '{' -> parseObj();
                case '[' -> parseArr();
                case '"' -> parseStr();
                case 't', 'f' -> parseBool();
                case 'n' -> parseNull();
                default -> parseNum();
            };
        }

        Map<String, Object> parseObj() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWs();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                skipWs();
                String key = parseStr();
                skipWs();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw new IllegalArgumentException("expected , or }");
            }
            return map;
        }

        List<Object> parseArr() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWs();
            if (peek() == ']') {
                i++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw new IllegalArgumentException("expected , or ]");
                skipWs();
            }
            return list;
        }

        String parseStr() {
            skipWs();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            String hex = s.substring(i, i + 4);
                            i += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new IllegalArgumentException("bad escape");
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBool() {
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("bad literal");
        }

        Object parseNull() {
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            throw new IllegalArgumentException("bad literal");
        }

        Number parseNum() {
            int start = i;
            if (peek() == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            boolean isDouble = false;
            if (i < s.length() && s.charAt(i) == '.') {
                isDouble = true;
                i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                isDouble = true;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            String tok = s.substring(start, i);
            if (tok.isEmpty() || "-".equals(tok)) throw new IllegalArgumentException("bad number");
            return isDouble ? Double.parseDouble(tok) : (Number) Long.parseLong(tok);
        }

        void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        char peek() {
            if (i >= s.length()) throw new IllegalArgumentException("unexpected end");
            return s.charAt(i);
        }

        char next() {
            char c = peek();
            i++;
            return c;
        }

        void expect(char c) {
            skipWs();
            char actual = next();
            if (actual != c) throw new IllegalArgumentException("expected " + c + " got " + actual);
        }
    }
}
