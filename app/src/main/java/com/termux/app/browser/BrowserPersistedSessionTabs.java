package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserPersistedSessionTabs {

    private final String mSessionName;

    private final List<BrowserPersistedTab> mTabs;

    private final int mActiveTabIndex;

    public BrowserPersistedSessionTabs(
            @NonNull String sessionName, @NonNull List<BrowserPersistedTab> tabs, int activeTabIndex) {
        mSessionName = sessionName;
        mTabs = new ArrayList<>(tabs);
        mActiveTabIndex = clampIndex(activeTabIndex, mTabs.size());
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

    private static int clampIndex(int index, int size) {
        if (size == 0) return 0;
        if (index < 0) return 0;
        if (index >= size) return size - 1;
        return index;
    }
}
