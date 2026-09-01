package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BrowserSessionBookmarkStore {

    private final Map<String, BrowserPersistedSessionTabs> mSessions;

    public BrowserSessionBookmarkStore(@NonNull Map<String, BrowserPersistedSessionTabs> sessions) {
        mSessions = sessions;
    }

    @NonNull
    public BrowserBookmarkCollection load(@Nullable String sessionName) {
        if (sessionName == null) return new BrowserBookmarkCollection(new ArrayList<>());
        BrowserPersistedSessionTabs session = mSessions.get(sessionName);
        return session != null
            ? new BrowserBookmarkCollection(session.getBookmarks())
            : new BrowserBookmarkCollection(new ArrayList<>());
    }

    public void save(@Nullable String sessionName, @NonNull List<BrowserBookmark> bookmarks) {
        if (sessionName == null) return;
        BrowserPersistedSessionTabs current = mSessions.get(sessionName);
        if (current != null) {
            mSessions.put(sessionName, current.withBookmarks(bookmarks));
        } else {
            mSessions.put(sessionName,
                new BrowserPersistedSessionTabs(
                    sessionName, new ArrayList<>(), 0, bookmarks, new BrowserTabHistory(), null));
        }
    }
}
