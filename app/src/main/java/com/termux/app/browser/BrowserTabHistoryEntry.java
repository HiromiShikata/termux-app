package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserTabHistoryEntry {

    public static final int MAX_BODY_SNIPPET_LENGTH = 2048;

    private final String mUrl;

    private final String mTitle;

    private final String mBodySnippet;

    private final Long mClosedAtMillis;

    public BrowserTabHistoryEntry(@NonNull String url, @NonNull String title) {
        this(url, title, "");
    }

    public BrowserTabHistoryEntry(@NonNull String url, @NonNull String title, @NonNull String bodySnippet) {
        this(url, title, bodySnippet, null);
    }

    public BrowserTabHistoryEntry(
        @NonNull String url,
        @NonNull String title,
        @NonNull String bodySnippet,
        @Nullable Long closedAtMillis) {
        mUrl = url;
        mTitle = title.isEmpty() ? url : title;
        mBodySnippet = boundBodySnippet(bodySnippet);
        mClosedAtMillis = closedAtMillis;
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

    @Nullable
    public Long getClosedAtMillis() {
        return mClosedAtMillis;
    }

    @NonNull
    public BrowserTabHistoryEntry withClosedAtMillis(long closedAtMillis) {
        return new BrowserTabHistoryEntry(mUrl, mTitle, mBodySnippet, closedAtMillis);
    }

    @NonNull
    private static String boundBodySnippet(@NonNull String bodySnippet) {
        String collapsed = bodySnippet.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_BODY_SNIPPET_LENGTH) return collapsed;
        return collapsed.substring(0, MAX_BODY_SNIPPET_LENGTH);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BrowserTabHistoryEntry)) return false;
        BrowserTabHistoryEntry that = (BrowserTabHistoryEntry) other;
        return mUrl.equals(that.mUrl)
            && mTitle.equals(that.mTitle)
            && mBodySnippet.equals(that.mBodySnippet)
            && (mClosedAtMillis == null ? that.mClosedAtMillis == null : mClosedAtMillis.equals(that.mClosedAtMillis));
    }

    @Override
    public int hashCode() {
        int result = mUrl.hashCode();
        result = 31 * result + mTitle.hashCode();
        result = 31 * result + mBodySnippet.hashCode();
        result = 31 * result + (mClosedAtMillis == null ? 0 : mClosedAtMillis.hashCode());
        return result;
    }
}
