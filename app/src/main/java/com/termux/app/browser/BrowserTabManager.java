package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BrowserTabManager {

    public static final int MAX_TABS_PER_SESSION = 10;

    private final Map<String, List<BrowserTab>> mTabsBySessionHandle = new HashMap<>();

    private final Map<String, BrowserTab> mActiveTabBySessionHandle = new HashMap<>();

    @NonNull
    public List<BrowserTab> getTabs(@NonNull String sessionHandle) {
        List<BrowserTab> tabs = mTabsBySessionHandle.get(sessionHandle);
        if (tabs == null) {
            tabs = new ArrayList<>();
            mTabsBySessionHandle.put(sessionHandle, tabs);
        }
        return tabs;
    }

    public boolean canAddTab(@NonNull String sessionHandle) {
        return getTabs(sessionHandle).size() < MAX_TABS_PER_SESSION;
    }

    @Nullable
    public BrowserTab addTab(@NonNull String sessionHandle, @NonNull String url) {
        List<BrowserTab> tabs = getTabs(sessionHandle);
        if (tabs.size() >= MAX_TABS_PER_SESSION) return null;
        BrowserTab tab = new BrowserTab(sessionHandle, url);
        tabs.add(tab);
        mActiveTabBySessionHandle.put(sessionHandle, tab);
        return tab;
    }

    public void removeTab(@NonNull BrowserTab tab) {
        String sessionHandle = tab.getSessionHandle();
        List<BrowserTab> tabs = getTabs(sessionHandle);
        int removedIndex = tabs.indexOf(tab);
        if (removedIndex < 0) return;
        tabs.remove(removedIndex);

        BrowserTab activeTab = mActiveTabBySessionHandle.get(sessionHandle);
        if (activeTab == tab) {
            if (tabs.isEmpty()) {
                mActiveTabBySessionHandle.remove(sessionHandle);
            } else {
                int nextIndex = Math.min(removedIndex, tabs.size() - 1);
                mActiveTabBySessionHandle.put(sessionHandle, tabs.get(nextIndex));
            }
        }
    }

    @Nullable
    public BrowserTab getActiveTab(@NonNull String sessionHandle) {
        return mActiveTabBySessionHandle.get(sessionHandle);
    }

    public void setActiveTab(@NonNull BrowserTab tab) {
        mActiveTabBySessionHandle.put(tab.getSessionHandle(), tab);
    }

    public void removeSession(@NonNull String sessionHandle) {
        mTabsBySessionHandle.remove(sessionHandle);
        mActiveTabBySessionHandle.remove(sessionHandle);
    }
}
