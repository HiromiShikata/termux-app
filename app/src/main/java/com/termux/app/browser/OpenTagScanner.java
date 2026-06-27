package com.termux.app.browser;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OpenTagScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("open", OpenTagScanner::normalizeUrl);

    private final BareUrlScanner bareUrlScanner = new BareUrlScanner();

    private final Set<String> openedUrls = new HashSet<>();

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

    public List<String> urlsToOpen(String output) {
        List<String> urlsToOpen = new ArrayList<>();
        for (String url : outputTagScanner.extractValues(output)) {
            if (openedUrls.add(url)) {
                urlsToOpen.add(url);
            }
        }
        for (String url : bareUrlScanner.urlsToOpen(output)) {
            if (openedUrls.add(url)) {
                urlsToOpen.add(url);
            }
        }
        return urlsToOpen;
    }
}
