package tw.winlab.reportlab;

/** Orchestrates the export pipeline: header, row table, optional totals,
 * then the finishing pass. */
final class ExportMgr {
    private ExportMgr() {}

    static String export(Report r, String fmt) {
        String hdr = Hdr.build(r);
        String body = RowFmt.render(r.rows);
        String tail = "brief".equals(fmt) ? "" : "\n" + Agg.summarize(r.rows);
        String doc0 = hdr + "\n" + body + tail;
        return PostProc.finish(doc0, r);
    }
}
