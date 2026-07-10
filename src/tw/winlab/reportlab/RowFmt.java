package tw.winlab.reportlab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Turns the row payload a report was created with into a plain
 * pipe-delimited table, using the keys of the first row as the column
 * header. */
final class RowFmt {
    private RowFmt() {}

    static String render(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return "(no rows)";

        List<String> cols = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", cols)).append('\n');
        for (Map<String, Object> row : rows) {
            List<String> cells = new ArrayList<>();
            for (String c : cols) cells.add(String.valueOf(row.get(c)));
            sb.append(String.join(" | ", cells)).append('\n');
        }
        return sb.toString();
    }
}
