package tw.winlab.reportlab;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Builds the fixed banner every export starts with: when it was generated,
 * what it's called, and what period it covers. */
final class Hdr {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.US);

    private Hdr() {}

    static String build(Report r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generated: ").append(LocalDate.now()).append('\n');
        sb.append("Report: ").append(r.title).append('\n');
        sb.append("Period: ").append(fmt(r.periodStart)).append(" to ").append(fmt(r.periodEnd)).append('\n');
        sb.append("-".repeat(40));
        return sb.toString();
    }

    private static String fmt(String iso) {
        return LocalDate.parse(iso).format(STAMP);
    }
}
