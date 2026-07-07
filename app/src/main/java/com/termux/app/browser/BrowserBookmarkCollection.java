package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BrowserBookmarkCollection {

    private final List<BrowserBookmark> mBookmarks;

    public BrowserBookmarkCollection(@NonNull List<BrowserBookmark> bookmarks) {
        mBookmarks = new ArrayList<>(bookmarks);
    }

    @NonNull
    public List<BrowserBookmark> getBookmarks() {
        return Collections.unmodifiableList(new ArrayList<>(mBookmarks));
    }

    @NonNull
    public static List<BrowserBookmark> filtered(@NonNull String query, @NonNull List<BrowserBookmark> bookmarks) {
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) return new ArrayList<>(bookmarks);
        List<BrowserBookmark> matches = new ArrayList<>();
        for (BrowserBookmark bookmark : bookmarks) {
            if (bookmark.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || bookmark.getUrl().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                matches.add(bookmark);
            }
        }
        return matches;
    }

    public boolean contains(@NonNull String url) {
        for (BrowserBookmark bookmark : mBookmarks) {
            if (bookmark.getUrl().equals(url)) return true;
        }
        return false;
    }

    @NonNull
    public BrowserBookmarkCollection added(@NonNull BrowserBookmark bookmark) {
        if (contains(bookmark.getUrl())) return this;
        List<BrowserBookmark> updated = new ArrayList<>(mBookmarks);
        updated.add(bookmark);
        return new BrowserBookmarkCollection(updated);
    }

    @NonNull
    public BrowserBookmarkCollection removed(@NonNull String url) {
        List<BrowserBookmark> updated = new ArrayList<>();
        for (BrowserBookmark bookmark : mBookmarks) {
            if (!bookmark.getUrl().equals(url)) updated.add(bookmark);
        }
        return new BrowserBookmarkCollection(updated);
    }

    @NonNull
    public BrowserBookmarkCollection toggled(@NonNull BrowserBookmark bookmark) {
        if (contains(bookmark.getUrl())) return removed(bookmark.getUrl());
        return added(bookmark);
    }
}
