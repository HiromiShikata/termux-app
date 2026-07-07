package com.termux.app.browser;

import com.termux.app.outputtag.OutputTagScanner;
import com.termux.view.url.DetectedUrlSanitizer;

import java.util.ArrayList;
import java.util.List;

public final class BareUrlScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner(BareUrlScanner::extractLineSoleUrls);

    public static List<String> extractLineSoleUrls(String output) {
        List<String> urls = new ArrayList<>();
        if (output == null) return urls;

        for (String line : output.split("\n", -1)) {
            String url = lineSoleUrl(line);
            if (url != null) urls.add(url);
        }
        return urls;
    }

    private static String lineSoleUrl(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.indexOf(' ') >= 0 || trimmed.indexOf('\t') >= 0) return null;
        String sanitized = DetectedUrlSanitizer.sanitize(trimmed);
        if (sanitized == null) return null;
        if (!BrowserLinkLongPress.isOpenableLinkUrl(sanitized)) return null;
        return sanitized;
    }

    public List<String> urlsToOpen(String output) {
        return outputTagScanner.newValues(output);
    }
}
