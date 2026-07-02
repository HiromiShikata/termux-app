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
        List<BrowserTabHistoryEntry> updated = new ArrayList<>();
        updated.add(new BrowserTabHistoryEntry(url, title));
        for (BrowserTabHistoryEntry entry : mEntries) {
            if (!entry.getUrl().equals(url)) updated.add(entry);
        }
        return new BrowserSessionTabHistory(updated, mMaxEntries);
    }
}
