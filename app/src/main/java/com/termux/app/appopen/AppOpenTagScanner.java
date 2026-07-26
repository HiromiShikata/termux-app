package com.termux.app.appopen;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.List;
import java.util.regex.Pattern;

public final class AppOpenTagScanner {

    private static final Pattern PACKAGE_ID_PATTERN =
        Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+");

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("app-open", AppOpenTagScanner::normalizePackageId);

    public static List<String> extractPackageIds(String output) {
        return new OutputTagScanner("app-open", AppOpenTagScanner::normalizePackageId).extractValues(output);
    }

    public static String normalizePackageId(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        if (!PACKAGE_ID_PATTERN.matcher(trimmed).matches()) return null;
        return trimmed;
    }

    public List<String> packageIdsToLaunch(String output) {
        return outputTagScanner.newValues(output);
    }
}
