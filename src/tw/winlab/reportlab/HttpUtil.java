package tw.winlab.reportlab;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small helpers shared by every route handler: JSON responses, plain-text
 * responses, body reading, bearer-token auth, path-segment splitting, and
 * query-string lookups. */
public final class HttpUtil {
    private static final Pattern BEARER_PATTERN = Pattern.compile("^Bearer\\s+(.+)$");

    private HttpUtil() {}

    public static void respond(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = JsonUtil.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void respondText(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            is.transferTo(buf);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    /** Returns the userId for a valid "Bearer test-&lt;id&gt;" header, or null
     * if the header is absent or malformed. Tokens without the "test-"
     * prefix are used as-is. */
    public static String parseAuth(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null) return null;
        Matcher m = BEARER_PATTERN.matcher(header.trim());
        if (!m.matches()) return null;
        String token = m.group(1).trim();
        if (token.isEmpty()) return null;
        return token.startsWith("test-") ? token.substring("test-".length()) : token;
    }

    /** Returns the userId, or writes a 401 response and returns null. */
    public static String requireAuth(HttpExchange ex) throws IOException {
        String uid = parseAuth(ex);
        if (uid == null) {
            respond(ex, 401, Map.of("error", "unauthorized"));
            return null;
        }
        return uid;
    }

    /** Segments of the request path after the registered context prefix. For
     * a context registered at "/reports" and a request to
     * "/reports/rpt_1/export" this returns ["rpt_1", "export"]. */
    public static String[] subPath(HttpExchange ex) {
        String contextPath = ex.getHttpContext().getPath();
        String full = ex.getRequestURI().getPath();
        String rest = full.substring(contextPath.length());
        while (rest.startsWith("/")) rest = rest.substring(1);
        while (rest.endsWith("/")) rest = rest.substring(0, rest.length() - 1);
        if (rest.isEmpty()) return new String[0];
        return rest.split("/");
    }

    /** Looks up a query-string parameter by name, returning {@code fallback}
     * if it's absent. Only handles the flat {@code key=value&...} shape this
     * service's endpoints actually use. */
    public static String queryParam(HttpExchange ex, String name, String fallback) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null || q.isEmpty()) return fallback;
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            String k = part.substring(0, eq);
            String v = part.substring(eq + 1);
            if (k.equals(name)) return java.net.URLDecoder.decode(v, StandardCharsets.UTF_8);
        }
        return fallback;
    }
}
