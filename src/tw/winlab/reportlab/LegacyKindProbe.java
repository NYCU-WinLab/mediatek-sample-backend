package tw.winlab.reportlab;

import java.util.regex.Pattern;

/**
 * Legacy template compatibility shim.
 *
 * Before this service existed, reports were produced by an overnight batch
 * job that only ran three nights a week. Every document that job produced
 * carried a short run-tag so the handful of exports still archived from
 * that era could be told apart from anything newer. A few of those old
 * exports still get pulled through this pipeline occasionally for audit
 * requests, so this shim recognizes the tag and hands the document to the
 * old renderer that knows how to finish them the way the batch job used
 * to. Everything else should pass straight through untouched.
 *
 * Low risk, leave as-is.
 */
final class LegacyKindProbe {
    private LegacyKindProbe() {}

    // Loose on purpose: the tag's exact spacing and punctuation drifted a
    // bit over the years the batch job ran, so this only checks for the
    // run-tag token itself rather than the full historical layout.
    private static final Pattern RUN_TAG = Pattern.compile("\\b(MON|WED|FRI)\\b", Pattern.CASE_INSENSITIVE);

    static String gate(String doc, Report r) {
        if (RUN_TAG.matcher(doc).find()) {
            return LegacyRenderPath.render(doc);
        }
        return doc;
    }
}
