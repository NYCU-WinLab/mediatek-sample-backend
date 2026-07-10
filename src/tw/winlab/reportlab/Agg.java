package tw.winlab.reportlab;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes the "Totals" block appended to full-format exports. Only
 * numeric columns get totalled; anything else is ignored for this
 * purpose. */
final class Agg {
    private Agg() {}

    static String summarize(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return "Totals: (no data)";

        Map<String, Double> sums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                Object v = e.getValue();
                if (!(v instanceof Number)) continue;
                double n = ((Number) v).doubleValue();
                String k = e.getKey();
                sums.merge(k, n, Double::sum);
                counts.merge(k, 1, Integer::sum);
            }
        }

        if (sums.isEmpty()) return "Totals: (no numeric columns)";

        StringBuilder sb = new StringBuilder("Totals:\n");
        for (Map.Entry<String, Double> e : sums.entrySet()) {
            String k = e.getKey();
            double sum = e.getValue();
            int n = counts.get(k);
            double avg = n == 0 ? 0 : sum / n;
            sb.append("  ").append(k).append(": sum=").append(fmt(sum)).append(" avg=").append(fmt(avg)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }
}
