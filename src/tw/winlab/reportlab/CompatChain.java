package tw.winlab.reportlab;

/**
 * Runs a finished document through a short list of compatibility passes
 * before it ships. Most of these exist for old clients that expect quirky
 * formatting; none should matter for anything produced by the current
 * pipeline, but they're cheap so they stay wired in.
 */
final class CompatChain {
    private CompatChain() {}

    static String apply(String doc, Report r) {
        String out = doc;
        out = stripBom(out);
        out = crlfNormalize(out);
        out = LegacyKindProbe.gate(out, r);
        return out;
    }

    private static String stripBom(String s) {
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    private static String crlfNormalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
