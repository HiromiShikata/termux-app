package com.termux.app.browser;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

public final class BrowserProjectOverviewPage {

    private static final Pattern PROJECT_BOARD_URL_PATTERN =
        Pattern.compile("https?://github\\.com/[^/]+/[^/]+/projects/\\d+");

    public static boolean isOverviewUrl(@Nullable String url) {
        if (url == null || url.isEmpty()) return false;
        return PROJECT_BOARD_URL_PATTERN.matcher(url).find();
    }

    private BrowserProjectOverviewPage() {
    }
}
