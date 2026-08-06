package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BrowserTabManager {

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

    @NonNull
    public BrowserTab addTab(@NonNull String sessionHandle, @NonNull String url) {
        List<BrowserTab> tabs = getTabs(sessionHandle);
        BrowserTab tab = new BrowserTab(sessionHandle, url);
        tabs.add(tab);
        mActiveTabBySessionHandle.put(sessionHandle, tab);
        return tab;
    }

    @NonNull
    public BrowserTab addBackgroundTab(@NonNull String sessionHandle, @NonNull String url) {
        List<BrowserTab> tabs = getTabs(sessionHandle);
        BrowserTab tab = new BrowserTab(sessionHandle, url);
        tabs.add(tab);
        if (mActiveTabBySessionHandle.get(sessionHandle) == null) {
            mActiveTabBySessionHandle.put(sessionHandle, tab);
        }
        return tab;
    }

    @NonNull
    public BrowserTab insertTab(@NonNull String sessionHandle, @NonNull String url, int index) {
        List<BrowserTab> tabs = getTabs(sessionHandle);
        BrowserTab tab = new BrowserTab(sessionHandle, url);
        int boundedIndex = index;
        if (boundedIndex < 0) boundedIndex = 0;
        if (boundedIndex > tabs.size()) boundedIndex = tabs.size();
        tabs.add(boundedIndex, tab);
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

    @NonNull
    public BrowserTab attachOrActivateTab(@NonNull String sessionHandle, @NonNull String url) {
        BrowserTab existingTab = findTabByUrl(sessionHandle, url);
        if (existingTab == null) return addBackgroundTab(sessionHandle, url);
        if (getActiveTab(sessionHandle) == null) setActiveTab(existingTab);
        return existingTab;
    }

    @Nullable
    public BrowserTab findTabByUrl(@NonNull String sessionHandle, @NonNull String url) {
        for (BrowserTab tab : getTabs(sessionHandle)) {
            if (tab.getUrl().equals(url)) return tab;
        }
        return null;
    }

    @Nullable
    public BrowserTab getActiveTab(@NonNull String sessionHandle) {
        return mActiveTabBySessionHandle.get(sessionHandle);
    }

    public void setActiveTab(@NonNull BrowserTab tab) {
        mActiveTabBySessionHandle.put(tab.getSessionHandle(), tab);
    }

    public int getActiveTabIndex(@NonNull String sessionHandle) {
        List<BrowserTab> tabs = getTabs(sessionHandle);
        BrowserTab activeTab = mActiveTabBySessionHandle.get(sessionHandle);
        if (activeTab == null) return -1;
        return tabs.indexOf(activeTab);
    }

    public boolean hasTabs(@NonNull String sessionHandle) {
        List<BrowserTab> tabs = mTabsBySessionHandle.get(sessionHandle);
        return tabs != null && !tabs.isEmpty();
    }

    public int getTotalOpenTabCount() {
        int total = 0;
        for (List<BrowserTab> tabs : mTabsBySessionHandle.values()) {
            total += tabs.size();
        }
        return total;
    }

    public void restoreTabs(
            @NonNull String sessionHandle,
            @NonNull List<BrowserPersistedTab> persistedTabs,
            int activeTabIndex) {
        if (persistedTabs.isEmpty() || hasTabs(sessionHandle)) return;
        List<BrowserTab> tabs = getTabs(sessionHandle);
        for (BrowserPersistedTab persistedTab : persistedTabs) {
            BrowserTab tab = new BrowserTab(sessionHandle, persistedTab.getUrl());
            tab.setTitle(persistedTab.getTitle());
            tab.setDesktopMode(persistedTab.isDesktopMode());
            tabs.add(tab);
        }
        int boundedIndex = activeTabIndex;
        if (boundedIndex < 0) boundedIndex = 0;
        if (boundedIndex >= tabs.size()) boundedIndex = tabs.size() - 1;
        mActiveTabBySessionHandle.put(sessionHandle, tabs.get(boundedIndex));
    }

    public void removeSession(@NonNull String sessionHandle) {
        mTabsBySessionHandle.remove(sessionHandle);
        mActiveTabBySessionHandle.remove(sessionHandle);
    }
}
