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

    @NonNull
    public String serialize(@NonNull List<BrowserPersistedSessionTabs> sessionTabs) throws JSONException {
        JSONArray sessionsArray = new JSONArray();
        for (BrowserPersistedSessionTabs session : sessionTabs) {
            if (session.getTabs().isEmpty()) continue;
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
            JSONArray tabsArray = sessionObject.getJSONArray(KEY_TABS);
            List<BrowserPersistedTab> tabs = new ArrayList<>();
            for (int tabIndex = 0; tabIndex < tabsArray.length(); tabIndex++) {
                JSONObject tabObject = tabsArray.getJSONObject(tabIndex);
                String url = tabObject.getString(KEY_URL);
                if (url.isEmpty()) continue;
                String title = tabObject.optString(KEY_TITLE, url);
                boolean desktopMode = tabObject.optBoolean(KEY_DESKTOP_MODE, true);
                tabs.add(new BrowserPersistedTab(url, title, desktopMode));
            }
            if (tabs.isEmpty()) continue;
            sessionTabs.add(new BrowserPersistedSessionTabs(sessionName, tabs, activeTabIndex));
        }
        return sessionTabs;
    }
}
