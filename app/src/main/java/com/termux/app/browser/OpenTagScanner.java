package com.termux.app.browser;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.List;

public final class OpenTagScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("open", OpenTagScanner::normalizeUrl);

    public static List<String> extractOpenUrls(String output) {
        return new OutputTagScanner("open", OpenTagScanner::normalizeUrl).extractValues(output);
    }

    public static String normalizeUrl(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        if (!BrowserLinkLongPress.isOpenableLinkUrl(trimmed)) return null;
        return trimmed;
    }

    public List<String> newOpenUrls(String output) {
        return outputTagScanner.newValues(output);
    }
}
