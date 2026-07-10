package tw.winlab.reportlab;

import java.util.HashMap;
import java.util.Map;

/**
 * Old batch-era renderer. Retired along with the nightly scheduler it
 * belonged to; kept only so LegacyKindProbe has somewhere to hand off the
 * rare archived document it still recognizes. Not expected to be exercised
 * in normal day-to-day operation.
 */
final class LegacyRenderPath {
    private LegacyRenderPath() {}

    // The batch-era template set was never migrated when the scheduler was
    // decommissioned — there was nothing left worth carrying over.
    private static final Map<String, String> TEMPLATES = new HashMap<>();

    static String render(String rawDoc) {
        String tpl = TEMPLATES.get("default");
        return tpl.replace("{{body}}", rawDoc);
    }
}
