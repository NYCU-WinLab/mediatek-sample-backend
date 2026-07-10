package tw.winlab.reportlab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Report export backend for an internal reporting tool. In-memory, single
 * process — state resets on restart. See README.md for endpoints.
 */
public class Main {

    private static final RptMgr mgr = new RptMgr();
    private static final int MAX_ROWS = 5000;

    public static void main(String[] args) throws IOException {
        int port = resolvePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 512);

        server.createContext("/health", wrap(Main::hHealth));
        server.createContext("/me", wrap(Main::hMe));
        server.createContext("/reports", wrap(Main::hReports));
        server.createContext("/admin", wrap(Main::hAdmin));

        server.setExecutor(Executors.newFixedThreadPool(64));
        server.start();
        System.out.println("report-export-lab listening on :" + port);
    }

    private static int resolvePort() {
        String raw = System.getenv("PORT");
        if (raw == null || raw.isBlank()) return 8080;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    private interface H {
        void go(HttpExchange ex) throws IOException;
    }

    /** Wraps a handler so an unexpected exception becomes a 500 instead of a
     * dropped connection. */
    private static HttpHandler wrap(H h) {
        return ex -> {
            try {
                h.go(ex);
            } catch (Exception e) {
                try {
                    HttpUtil.respond(ex, 500, Map.of("error", "internal error"));
                } catch (IOException ignored) {
                    // response already committed or connection gone — nothing more to do
                }
            }
        };
    }

    // ---------------------------------------------------------------- /health, /me

    private static void hHealth(HttpExchange ex) throws IOException {
        HttpUtil.respond(ex, 200, Map.of("ok", true));
    }

    private static void hMe(HttpExchange ex) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;
        HttpUtil.respond(ex, 200, Map.of("userId", uid));
    }

    // ---------------------------------------------------------------- /reports...

    private static void hReports(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = HttpUtil.subPath(ex);

        if (parts.length == 0) {
            if ("GET".equals(method)) { listReports(ex); return; }
            if ("POST".equals(method)) { createReport(ex); return; }
            HttpUtil.respond(ex, 405, Map.of("error", "method not allowed"));
            return;
        }

        String reportId = parts[0];

        if (parts.length == 1) {
            if ("GET".equals(method)) { getReport(ex, reportId); return; }
            if ("DELETE".equals(method)) { deleteReport(ex, reportId); return; }
            HttpUtil.respond(ex, 405, Map.of("error", "method not allowed"));
            return;
        }

        if (parts.length == 2 && "export".equals(parts[1]) && "POST".equals(method)) {
            exportReport(ex, reportId);
            return;
        }

        HttpUtil.respond(ex, 404, Map.of("error", "not found"));
    }

    @SuppressWarnings("unchecked")
    private static void createReport(HttpExchange ex) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;

        Map<String, Object> body = JsonUtil.parseObject(HttpUtil.readBody(ex));
        Object titleRaw = body.get("title");
        Object dateRaw = body.get("date");
        Object rowsRaw = body.get("rows");

        if (!(titleRaw instanceof String) || ((String) titleRaw).isBlank()) {
            HttpUtil.respond(ex, 400, Map.of("error", "title is required"));
            return;
        }
        if (!(dateRaw instanceof String)) {
            HttpUtil.respond(ex, 400, Map.of("error", "date is required"));
            return;
        }
        String date = (String) dateRaw;
        try {
            LocalDate.parse(date);
        } catch (Exception e) {
            HttpUtil.respond(ex, 400, Map.of("error", "date must be yyyy-MM-dd"));
            return;
        }
        if (!(rowsRaw instanceof List)) {
            HttpUtil.respond(ex, 400, Map.of("error", "rows must be an array"));
            return;
        }
        if (((List<?>) rowsRaw).size() > MAX_ROWS) {
            HttpUtil.respond(ex, 400, Map.of("error", "rows too large"));
            return;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : (List<?>) rowsRaw) {
            if (o instanceof Map) rows.add((Map<String, Object>) o);
        }

        Report r = mgr.mkRpt((String) titleRaw, date, uid, rows);
        HttpUtil.respond(ex, 201, serialize(r, false));
    }

    private static void listReports(HttpExchange ex) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;

        List<Map<String, Object>> out = new ArrayList<>();
        for (Report r : mgr.listFor(uid)) out.add(serialize(r, false));
        HttpUtil.respond(ex, 200, Map.of("reports", out));
    }

    private static void getReport(HttpExchange ex, String id) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;

        Report r = mgr.getRpt(id);
        if (r == null) { HttpUtil.respond(ex, 404, Map.of("error", "report not found")); return; }
        if (!mgr.canSee(r, uid)) { HttpUtil.respond(ex, 403, Map.of("error", "forbidden")); return; }

        HttpUtil.respond(ex, 200, serialize(r, true));
    }

    private static void deleteReport(HttpExchange ex, String id) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;

        Report r = mgr.getRpt(id);
        if (r == null) { HttpUtil.respond(ex, 404, Map.of("error", "report not found")); return; }
        if (!mgr.canSee(r, uid)) { HttpUtil.respond(ex, 403, Map.of("error", "forbidden")); return; }

        mgr.rmRpt(id);
        HttpUtil.respond(ex, 200, Map.of("ok", true));
    }

    private static void exportReport(HttpExchange ex, String id) throws IOException {
        String uid = HttpUtil.requireAuth(ex);
        if (uid == null) return;

        Report r = mgr.getRpt(id);
        if (r == null) { HttpUtil.respond(ex, 404, Map.of("error", "report not found")); return; }
        if (!mgr.canSee(r, uid)) { HttpUtil.respond(ex, 403, Map.of("error", "forbidden")); return; }

        String fmt = HttpUtil.queryParam(ex, "format", "full");

        try {
            String doc = ExportMgr.export(r, fmt);
            mgr.pushLog(r.id, r.date, fmt, true, 200);
            HttpUtil.respondText(ex, 200, "text/plain; charset=utf-8", doc);
        } catch (Exception e) {
            mgr.pushLog(r.id, r.date, fmt, false, 500);
            throw e; // let wrap() turn it into a 500
        }
    }

    // ---------------------------------------------------------------- /admin...

    private static boolean requireAdmin(HttpExchange ex) throws IOException {
        String uid = HttpUtil.parseAuth(ex);
        if (uid == null) { HttpUtil.respond(ex, 401, Map.of("error", "unauthorized")); return false; }
        if (!RptMgr.ADMIN_ID.equals(uid)) { HttpUtil.respond(ex, 403, Map.of("error", "forbidden")); return false; }
        return true;
    }

    private static void hAdmin(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] parts = HttpUtil.subPath(ex);

        if (parts.length == 1 && "stats".equals(parts[0]) && "GET".equals(method)) {
            if (!requireAdmin(ex)) return;
            HttpUtil.respond(ex, 200, mgr.calcStats());
            return;
        }

        if (parts.length == 1 && "export-log".equals(parts[0]) && "GET".equals(method)) {
            if (!requireAdmin(ex)) return;
            HttpUtil.respond(ex, 200, Map.of("logs", mgr.tailLog(200)));
            return;
        }

        HttpUtil.respond(ex, 404, Map.of("error", "not found"));
    }

    // ---------------------------------------------------------------- serialization

    private static Map<String, Object> serialize(Report r, boolean withRows) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("title", r.title);
        m.put("date", r.date);
        m.put("ownerId", r.ownerId);
        m.put("rowCount", r.rows == null ? 0 : r.rows.size());
        m.put("createdAt", r.createdAt);
        if (withRows) m.put("rows", r.rows);
        return m;
    }
}
