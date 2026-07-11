package tw.winlab.reportlab;

import java.util.List;
import java.util.Map;

/** Plain data holder for one report: what it's called, what period it
 * covers, who owns it, and the row payload it was created with. */
public class Report {
    public String id;
    public String title;
    public String periodStart;
    public String periodEnd;
    public String ownerId;
    public String createdAt;
    public List<Map<String, Object>> rows;

    public Report(String id, String title, String periodStart, String periodEnd, String ownerId, String createdAt, List<Map<String, Object>> rows) {
        this.id = id;
        this.title = title;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.rows = rows;
    }
}
