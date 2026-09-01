package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BrowserPersistedTabsSerializer {

    private static final String KEY_SESSION_NAME = "sessionName";
    private static final String KEY_ACTIVE_TAB_INDEX = "activeTabIndex";
    private static final String KEY_TABS = "tabs";
    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESKTOP_MODE = "desktopMode";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_DELETED_AT_MILLIS = "deletedAtMillis";

    private final BrowserBookmarkSerializer mBookmarkSerializer = new BrowserBookmarkSerializer();

    private final BrowserTabHistorySerializer mHistorySerializer = new BrowserTabHistorySerializer();

    @NonNull
    public String serialize(@NonNull List<BrowserPersistedSessionTabs> sessionTabs) throws JSONException {
        JSONArray sessionsArray = new JSONArray();
        for (BrowserPersistedSessionTabs session : sessionTabs) {
            boolean hasTabs = !session.getTabs().isEmpty();
            boolean hasBookmarks = !session.getBookmarks().isEmpty();
            boolean hasHistory = !session.getHistory().isEmpty();
            boolean hasDeletedMarker = session.isDeleted();
            if (!hasTabs && !hasBookmarks && !hasHistory && !hasDeletedMarker) continue;
            JSONObject sessionObject = new JSONObject();
            sessionObject.put(KEY_SESSION_NAME, session.getSessionName());
            sessionObject.put(KEY_ACTIVE_TAB_INDEX, session.getActiveTabIndex());
            JSONArray tabsArray = new JSONArray();
            for (BrowserPersistedTab tab : session.getTabs()) {
                JSONObject tabObject = new JSONObject();
                tabObject.put(KEY_URL, tab.getUrl());
                tabObject.put(KEY_TITLE, tab.getTitle());
                tabObject.put(KEY_DESKTOP_MODE, tab.isDesktopMode());
                tabsArray.put(tabObject);
            }
            sessionObject.put(KEY_TABS, tabsArray);
            if (hasBookmarks) {
                sessionObject.put(KEY_BOOKMARKS,
                    new JSONArray(mBookmarkSerializer.serialize(session.getBookmarks())));
            }
            if (hasHistory) {
                sessionObject.put(KEY_HISTORY,
                    new JSONArray(mHistorySerializer.serialize(session.getHistory())));
            }
            if (hasDeletedMarker) {
                sessionObject.put(KEY_DELETED_AT_MILLIS, session.getDeletedAtMillis().longValue());
            }
            sessionsArray.put(sessionObject);
        }
        return sessionsArray.toString();
    }

    @NonNull
    public List<BrowserPersistedSessionTabs> deserialize(@Nullable String serialized) throws JSONException {
        List<BrowserPersistedSessionTabs> sessionTabs = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) return sessionTabs;

        JSONArray sessionsArray = new JSONArray(serialized);
        for (int sessionIndex = 0; sessionIndex < sessionsArray.length(); sessionIndex++) {
            JSONObject sessionObject = sessionsArray.getJSONObject(sessionIndex);
            String sessionName = sessionObject.getString(KEY_SESSION_NAME);
            if (sessionName.isEmpty()) continue;
            int activeTabIndex = sessionObject.optInt(KEY_ACTIVE_TAB_INDEX, 0);
            JSONArray tabsArray = sessionObject.optJSONArray(KEY_TABS);
            List<BrowserPersistedTab> tabs = new ArrayList<>();
            if (tabsArray != null) {
                for (int tabIndex = 0; tabIndex < tabsArray.length(); tabIndex++) {
                    JSONObject tabObject = tabsArray.getJSONObject(tabIndex);
                    String url = tabObject.getString(KEY_URL);
                    if (url.isEmpty()) continue;
                    String title = tabObject.optString(KEY_TITLE, url);
                    boolean desktopMode = tabObject.optBoolean(KEY_DESKTOP_MODE, true);
                    tabs.add(new BrowserPersistedTab(url, title, desktopMode));
                }
            }
            JSONArray bookmarksArray = sessionObject.optJSONArray(KEY_BOOKMARKS);
            List<BrowserBookmark> bookmarks = bookmarksArray != null
                ? mBookmarkSerializer.deserialize(bookmarksArray.toString())
                : new ArrayList<>();
            JSONArray historyArray = sessionObject.optJSONArray(KEY_HISTORY);
            BrowserTabHistory history = historyArray != null
                ? mHistorySerializer.deserialize(historyArray.toString(), BrowserTabHistory.DEFAULT_MAX_ENTRIES)
                : new BrowserTabHistory();
            Long deletedAtMillis = sessionObject.has(KEY_DELETED_AT_MILLIS)
                ? sessionObject.getLong(KEY_DELETED_AT_MILLIS)
                : null;
            sessionTabs.add(new BrowserPersistedSessionTabs(
                sessionName, tabs, activeTabIndex, bookmarks, history, deletedAtMillis));
        }
        return sessionTabs;
    }

    @NonNull
    public static List<BrowserPersistedSessionTabs> pruneStaleDeleted(
            @NonNull List<BrowserPersistedSessionTabs> sessions, long currentTimeMillis) {
        List<BrowserPersistedSessionTabs> result = new ArrayList<>();
        for (BrowserPersistedSessionTabs session : sessions) {
            if (session.isStaleAt(currentTimeMillis)) continue;
            result.add(session);
        }
        return result;
    }
}
