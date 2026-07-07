package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BrowserNewTabList {

    private BrowserNewTabList() {
    }

    @NonNull
    public static List<BrowserNewTabEntry> combined(
        @NonNull String query,
        @NonNull List<BrowserBookmark> bookmarks,
        @NonNull List<BrowserTabHistoryEntry> historyEntries) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        List<BrowserNewTabEntry> combined = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();
        for (BrowserBookmark bookmark : bookmarks) {
            if (seenUrls.add(bookmark.getUrl()) && matches(normalizedQuery, bookmark.getTitle(), bookmark.getUrl())) {
                combined.add(new BrowserNewTabEntry(bookmark.getUrl(), bookmark.getTitle(), true));
            }
        }
        for (BrowserTabHistoryEntry entry : sortedByCloseOrder(historyEntries)) {
            if (seenUrls.add(entry.getUrl()) && matches(normalizedQuery, entry.getTitle(), entry.getUrl())) {
                combined.add(new BrowserNewTabEntry(entry.getUrl(), entry.getTitle(), false));
            }
        }
        return combined;
    }

    @NonNull
    private static List<BrowserTabHistoryEntry> sortedByCloseOrder(
        @NonNull List<BrowserTabHistoryEntry> historyEntries) {
        List<BrowserTabHistoryEntry> sorted = new ArrayList<>(historyEntries);
        sorted.sort(Comparator.comparing(
            BrowserTabHistoryEntry::getClosedAtMillis,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    private static boolean matches(@NonNull String normalizedQuery, @NonNull String title, @NonNull String url) {
        if (normalizedQuery.isEmpty()) return true;
        return title.toLowerCase(Locale.ROOT).contains(normalizedQuery)
            || url.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
