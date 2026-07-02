package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserTabHistoryEntry {

    public static final int MAX_BODY_SNIPPET_LENGTH = 2048;

    private final String mUrl;

    private final String mTitle;

    private final String mBodySnippet;

    public BrowserTabHistoryEntry(@NonNull String url, @NonNull String title) {
        this(url, title, "");
    }

    public BrowserTabHistoryEntry(@NonNull String url, @NonNull String title, @NonNull String bodySnippet) {
        mUrl = url;
        mTitle = title.isEmpty() ? url : title;
        mBodySnippet = boundBodySnippet(bodySnippet);
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public String getBodySnippet() {
        return mBodySnippet;
    }

    @NonNull
    private static String boundBodySnippet(@NonNull String bodySnippet) {
        String collapsed = bodySnippet.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_BODY_SNIPPET_LENGTH) return collapsed;
        return collapsed.substring(0, MAX_BODY_SNIPPET_LENGTH);
    }
}
