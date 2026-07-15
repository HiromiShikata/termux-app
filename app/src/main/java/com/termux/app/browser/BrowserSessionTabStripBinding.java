package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserSessionTabStripBinding {

    private final List<BrowserTab> mTabs;
    private final BrowserTab mActiveTab;

    private BrowserSessionTabStripBinding(@NonNull List<BrowserTab> tabs, @Nullable BrowserTab activeTab) {
        this.mTabs = tabs;
        this.mActiveTab = activeTab;
    }

    @NonNull
    public List<BrowserTab> getTabs() {
        return mTabs;
    }

    @Nullable
    public BrowserTab getActiveTab() {
        return mActiveTab;
    }

    public boolean isEmpty() {
        return mTabs.isEmpty();
    }

    @NonNull
    public static BrowserSessionTabStripBinding empty() {
        return new BrowserSessionTabStripBinding(Collections.emptyList(), null);
    }

    @NonNull
    public static BrowserSessionTabStripBinding forSession(
            @Nullable String sessionHandle, @NonNull BrowserTabManager tabManager) {
        if (sessionHandle == null) {
            return empty();
        }
        List<BrowserTab> tabs = new ArrayList<>(tabManager.getTabs(sessionHandle));
        BrowserTab activeTab = tabManager.getActiveTab(sessionHandle);
        return new BrowserSessionTabStripBinding(tabs, activeTab);
    }
}
