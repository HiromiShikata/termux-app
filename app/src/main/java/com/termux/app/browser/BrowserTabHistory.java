package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BrowserTabHistory {

    public static final int DEFAULT_MAX_ENTRIES = 1000;

    private final List<BrowserTabHistoryEntry> mEntries;

    private final int mMaxEntries;

    public BrowserTabHistory() {
        this(new ArrayList<>(), DEFAULT_MAX_ENTRIES);
    }

    public BrowserTabHistory(@NonNull List<BrowserTabHistoryEntry> entries, int maxEntries) {
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
    public BrowserTabHistory recorded(@NonNull String url, @NonNull String title) {
        return recorded(url, title, "");
    }

    @NonNull
    public BrowserTabHistory recorded(@NonNull String url, @NonNull String title, @NonNull String bodySnippet) {
        if (url.isEmpty()) return this;
        String canonicalUrl = canonicalizeUrl(url);
        String key = deduplicationKey(canonicalUrl);
        String preferredTitle = preferredTitle(canonicalUrl, title, key);
        String preferredBodySnippet = preferredBodySnippet(bodySnippet, key);
        List<BrowserTabHistoryEntry> updated = new ArrayList<>();
        updated.add(new BrowserTabHistoryEntry(canonicalUrl, preferredTitle, preferredBodySnippet));
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (!deduplicationKey(entry.getUrl()).equals(key)) updated.add(entry);
        }
        return new BrowserTabHistory(updated, mMaxEntries);
    }

    @NonNull
    public List<BrowserTabHistoryEntry> filtered(@NonNull String query) {
        List<String> tokens = tokenize(query);
        List<BrowserTabHistoryEntry> matches = new ArrayList<>();
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (matchesAllTokens(entry, tokens)) matches.add(entry);
        }
        return Collections.unmodifiableList(matches);
    }

    private static boolean matchesAllTokens(@NonNull BrowserTabHistoryEntry entry, @NonNull List<String> tokens) {
        if (tokens.isEmpty()) return true;
        String haystack = searchableText(entry);
        for (String token : tokens) {
            if (!haystack.contains(token)) return false;
        }
        return true;
    }

    @NonNull
    private static String searchableText(@NonNull BrowserTabHistoryEntry entry) {
        return (entry.getTitle() + " " + entry.getUrl() + " " + entry.getBodySnippet())
            .toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static List<String> tokenize(@NonNull String query) {
        List<String> tokens = new ArrayList<>();
        for (String token : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
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
    private String preferredBodySnippet(@NonNull String bodySnippet, @NonNull String key) {
        if (!bodySnippet.isEmpty()) return bodySnippet;
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (deduplicationKey(entry.getUrl()).equals(key) && !entry.getBodySnippet().isEmpty()) {
                return entry.getBodySnippet();
            }
        }
        return bodySnippet;
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
