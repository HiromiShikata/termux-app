package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserPersistedSessionTabs {

    private static final long DELETION_RETENTION_MS = 10L * 24 * 60 * 60 * 1000;

    private final String mSessionName;

    private final List<BrowserPersistedTab> mTabs;

    private final int mActiveTabIndex;

    private final List<BrowserBookmark> mBookmarks;

    private final BrowserTabHistory mHistory;

    @Nullable
    private final Long mDeletedAtMillis;

    public BrowserPersistedSessionTabs(
            @NonNull String sessionName, @NonNull List<BrowserPersistedTab> tabs, int activeTabIndex) {
        this(sessionName, tabs, activeTabIndex, new ArrayList<>(), new BrowserTabHistory(), null);
    }

    public BrowserPersistedSessionTabs(
            @NonNull String sessionName,
            @NonNull List<BrowserPersistedTab> tabs,
            int activeTabIndex,
            @NonNull List<BrowserBookmark> bookmarks,
            @NonNull BrowserTabHistory history,
            @Nullable Long deletedAtMillis) {
        mSessionName = sessionName;
        mTabs = new ArrayList<>(tabs);
        mActiveTabIndex = clampIndex(activeTabIndex, mTabs.size());
        mBookmarks = new ArrayList<>(bookmarks);
        mHistory = history;
        mDeletedAtMillis = deletedAtMillis;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
    }

    @NonNull
    public List<BrowserPersistedTab> getTabs() {
        return Collections.unmodifiableList(mTabs);
    }

    public int getActiveTabIndex() {
        return mActiveTabIndex;
    }

    @NonNull
    public List<BrowserBookmark> getBookmarks() {
        return Collections.unmodifiableList(mBookmarks);
    }

    @NonNull
    public BrowserTabHistory getHistory() {
        return mHistory;
    }

    @Nullable
    public Long getDeletedAtMillis() {
        return mDeletedAtMillis;
    }

    public boolean isDeleted() {
        return mDeletedAtMillis != null;
    }

    public boolean isStaleAt(long currentTimeMillis) {
        return mDeletedAtMillis != null && currentTimeMillis - mDeletedAtMillis > DELETION_RETENTION_MS;
    }

    public boolean hasRetainableData() {
        return !mBookmarks.isEmpty() || !mHistory.isEmpty();
    }

    @NonNull
    public BrowserPersistedSessionTabs withBookmarks(@NonNull List<BrowserBookmark> bookmarks) {
        return new BrowserPersistedSessionTabs(
            mSessionName, mTabs, mActiveTabIndex, bookmarks, mHistory, mDeletedAtMillis);
    }

    @NonNull
    public BrowserPersistedSessionTabs withHistory(@NonNull BrowserTabHistory history) {
        return new BrowserPersistedSessionTabs(
            mSessionName, mTabs, mActiveTabIndex, mBookmarks, history, mDeletedAtMillis);
    }

    @NonNull
    public BrowserPersistedSessionTabs withDeletedAtMillis(long deletedAtMillis) {
        return new BrowserPersistedSessionTabs(
            mSessionName, new ArrayList<>(), 0, mBookmarks, mHistory, deletedAtMillis);
    }

    @NonNull
    public BrowserPersistedSessionTabs withoutDeletedMarker() {
        return new BrowserPersistedSessionTabs(
            mSessionName, mTabs, mActiveTabIndex, mBookmarks, mHistory, null);
    }

    private static int clampIndex(int index, int size) {
        if (size == 0) return 0;
        if (index < 0) return 0;
        if (index >= size) return size - 1;
        return index;
    }
}
