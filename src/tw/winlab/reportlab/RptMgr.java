package tw.winlab.reportlab;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Everything to do with report storage, ownership checks, and the export
 * activity log lives here — it grew out of what used to be a couple of
 * separate classes and nobody's gotten around to splitting it back apart.
 */
public class RptMgr {
    public static final String ADMIN_ID = "admin";

    private final Map<String, Report> data = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> logz = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong gen = new AtomicLong(1);

    // ---------------------------------------------------------------- CRUD

    public Report mkRpt(String title, String periodStart, String periodEnd, String ownerId, List<Map<String, Object>> rows) {
        String id = "rpt_" + gen.getAndIncrement();
        Report r = new Report(id, title, periodStart, periodEnd, ownerId, Instant.now().toString(), rows);
        data.put(id, r);
        return r;
    }

    public Report getRpt(String id) {
        return data.get(id);
    }

    public boolean rmRpt(String id) {
        return data.remove(id) != null;
    }

    public List<Report> listFor(String userId) {
        List<Report> out = new ArrayList<>();
        for (Report r : data.values()) {
            boolean visible = ADMIN_ID.equals(userId) || r.ownerId.equals(userId);
            if (visible) out.add(r);
        }
        out.sort((a, b) -> a.id.compareTo(b.id));
        return out;
    }

    public boolean canSee(Report r, String userId) {
        if (r == null) return false;
        return ADMIN_ID.equals(userId) || r.ownerId.equals(userId);
    }

    // ---------------------------------------------------------------- export activity log

    public void pushLog(String reportId, String periodStart, String periodEnd, String fmt, boolean okFlag, int httpStatus) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("reportId", reportId);
        row.put("periodStart", periodStart);
        row.put("periodEnd", periodEnd);
        row.put("format", fmt);
        row.put("ok", okFlag);
        row.put("httpStatus", httpStatus);
        row.put("ts", Instant.now().toString());
        logz.add(row);
    }

    public List<Map<String, Object>> tailLog(int n) {
        int sz = logz.size();
        int from = Math.max(0, sz - n);
        synchronized (logz) {
            return new ArrayList<>(logz.subList(from, sz));
        }
    }

    // ---------------------------------------------------------------- stats

    public Map<String, Object> calcStats() {
        int totalReports = data.size();
        Set<String> owners = new HashSet<>();
        for (Report r : data.values()) owners.add(r.ownerId);

        int attempted = 0;
        int good = 0;
        int bad = 0;
        List<Map<String, Object>> logSnapshot;
        synchronized (logz) {
            logSnapshot = new ArrayList<>(logz);
        }
        for (Map<String, Object> row : logSnapshot) {
            attempted++;
            if (Boolean.TRUE.equals(row.get("ok"))) good++;
            else bad++;
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalReports", totalReports);
        m.put("totalUsers", owners.size());
        m.put("totalExportsAttempted", attempted);
        m.put("totalExportSuccesses", good);
        m.put("totalExportFailures", bad);
        return m;
    }
}
