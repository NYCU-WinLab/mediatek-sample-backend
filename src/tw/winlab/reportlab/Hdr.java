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
        LocalDate d = LocalDate.parse(r.date);
        StringBuilder sb = new StringBuilder();
        sb.append("Generated: ").append(d.format(STAMP)).append('\n');
        sb.append("Report: ").append(r.title).append('\n');
        sb.append("Period: ").append(r.date).append('\n');
        sb.append("-".repeat(40));
        return sb.toString();
    }
}
