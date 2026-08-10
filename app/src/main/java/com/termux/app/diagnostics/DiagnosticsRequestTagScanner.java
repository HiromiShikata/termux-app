package com.termux.app.diagnostics;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.List;

public final class DiagnosticsRequestTagScanner {

    public static final String TAG_NAME = "diagnostics-report";

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner(TAG_NAME, DiagnosticsRequestTagScanner::normalizeRequestLabel);

    public static String normalizeRequestLabel(String innerText) {
        if (innerText == null) return null;
        return innerText.trim();
    }

    public List<String> newReportRequests(String output) {
        return outputTagScanner.newValues(output);
    }
}
