package tw.winlab.reportlab;

/** Last pass over a finished document before it's handed back to the
 * caller: runs the client-compatibility fixups every export goes through,
 * then trims stray whitespace and stamps a closing line. */
final class PostProc {
    private PostProc() {}

    static String finish(String doc, Report r) {
        String out = CompatChain.apply(doc, r);
        return out.strip() + "\n" + "-".repeat(40) + "\nEnd of report\n";
    }
}
