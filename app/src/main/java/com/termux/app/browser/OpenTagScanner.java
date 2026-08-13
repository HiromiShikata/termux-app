package com.termux.app.browser;

import com.termux.app.outputtag.OutputTagScanner;
import com.termux.view.url.DetectedUrlSanitizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OpenTagScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("open", OpenTagScanner::normalizeUrl);

    private final Set<String> openedUrls = new HashSet<>();

    public static List<String> extractOpenUrls(String output) {
        return new OutputTagScanner("open", OpenTagScanner::normalizeUrl).extractValues(output);
    }

    public static String normalizeUrl(String innerText) {
        if (innerText == null) return null;
        String sanitized = DetectedUrlSanitizer.sanitize(urlRebuiltFrom(innerText), innerText);
        if (sanitized == null || sanitized.isEmpty()) return null;
        if (!BrowserLinkLongPress.isOpenableLinkUrl(sanitized)) return null;
        return sanitized;
    }

    private static String urlRebuiltFrom(String innerText) {
        StringBuilder rebuilt = new StringBuilder(innerText.length());
        for (int index = 0; index < innerText.length(); index++) {
            char character = innerText.charAt(index);
            if (!Character.isWhitespace(character)) rebuilt.append(character);
        }
        return rebuilt.toString();
    }

    public List<String> urlsToOpen(String output) {
        List<String> urlsToOpen = new ArrayList<>();
        for (String url : outputTagScanner.extractValues(output)) {
            if (openedUrls.add(url)) {
                urlsToOpen.add(url);
            }
        }
        return urlsToOpen;
    }

    public void rememberWithoutOpening(String output) {
        openedUrls.addAll(outputTagScanner.extractValues(output));
    }
}
