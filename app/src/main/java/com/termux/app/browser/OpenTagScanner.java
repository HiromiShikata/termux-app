package com.termux.app.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenTagScanner {

    private static final Pattern OPEN_BLOCK_PATTERN = Pattern.compile("<open>([\\s\\S]*?)</open>");

    private String lastOpenedUrl;

    public static List<String> extractOpenUrls(String output) {
        List<String> openUrls = new ArrayList<>();
        if (output == null) return openUrls;

        Matcher matcher = OPEN_BLOCK_PATTERN.matcher(output);
        while (matcher.find()) {
            String openUrl = normalizeUrl(matcher.group(1));
            if (openUrl != null) openUrls.add(openUrl);
        }
        return openUrls;
    }

    public static String normalizeUrl(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        if (trimmed.isEmpty()) return null;
        if (!BrowserLinkLongPress.isOpenableLinkUrl(trimmed)) return null;
        return trimmed;
    }

    public String newOpenUrl(String output) {
        List<String> openUrls = extractOpenUrls(output);
        if (openUrls.isEmpty()) return null;

        String latestOpenUrl = openUrls.get(openUrls.size() - 1);
        if (latestOpenUrl.equals(lastOpenedUrl)) return null;
        return latestOpenUrl;
    }

    public void markOpened(String openUrl) {
        lastOpenedUrl = openUrl;
    }
}
