package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserSessionTabHistory {

    public static final int DEFAULT_MAX_ENTRIES = 30;

    private final List<BrowserTabHistoryEntry> mEntries;

    private final int mMaxEntries;

    public BrowserSessionTabHistory() {
        this(new ArrayList<>(), DEFAULT_MAX_ENTRIES);
    }

    public BrowserSessionTabHistory(@NonNull List<BrowserTabHistoryEntry> entries, int maxEntries) {
        mMaxEntries = maxEntries < 1 ? 1 : maxEntries;
        List<BrowserTabHistoryEntry> bounded = new ArrayList<>(entries);
        while (bounded.size() > mMaxEntries) bounded.remove(bounded.size() - 1);
        mEntries = bounded;
    }

    @NonNull
    public List<BrowserTabHistoryEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(mEntries));
    }

    public boolean isEmpty() {
        return mEntries.isEmpty();
    }

    @NonNull
    public BrowserSessionTabHistory recorded(@NonNull String url, @NonNull String title) {
        if (url.isEmpty()) return this;
        String canonicalUrl = canonicalizeUrl(url);
        String key = deduplicationKey(canonicalUrl);
        String preferredTitle = preferredTitle(canonicalUrl, title, key);
        List<BrowserTabHistoryEntry> updated = new ArrayList<>();
        updated.add(new BrowserTabHistoryEntry(canonicalUrl, preferredTitle));
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (!deduplicationKey(entry.getUrl()).equals(key)) updated.add(entry);
        }
        return new BrowserSessionTabHistory(updated, mMaxEntries);
    }

    @NonNull
    private String preferredTitle(@NonNull String canonicalUrl, @NonNull String title, @NonNull String key) {
        String incomingTitle = title.isEmpty() ? canonicalUrl : title;
        if (!incomingTitle.equals(canonicalUrl)) return incomingTitle;
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (deduplicationKey(entry.getUrl()).equals(key) && !entry.getTitle().equals(entry.getUrl())) {
                return entry.getTitle();
            }
        }
        return incomingTitle;
    }

    @NonNull
    private static String canonicalizeUrl(@NonNull String url) {
        int queryOrFragmentStart = indexOfQueryOrFragment(url);
        if (queryOrFragmentStart >= 0) return url;
        int schemeEnd = url.indexOf("://");
        int pathStart = schemeEnd >= 0 ? url.indexOf('/', schemeEnd + 3) : url.indexOf('/');
        if (pathStart < 0) return url;
        if (url.length() - 1 == pathStart) return url.substring(0, pathStart);
        if (url.endsWith("/")) return url.substring(0, url.length() - 1);
        return url;
    }

    @NonNull
    private static String deduplicationKey(@NonNull String url) {
        return canonicalizeUrl(url);
    }

    private static int indexOfQueryOrFragment(@NonNull String url) {
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        if (query < 0) return fragment;
        if (fragment < 0) return query;
        return Math.min(query, fragment);
    }
}
