package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserClosedTab {

    private final String mSessionHandle;

    private final String mUrl;

    private final String mTitle;

    private final boolean mDesktopMode;

    private final int mListIndex;

    public BrowserClosedTab(
            @NonNull String sessionHandle,
            @NonNull String url,
            @NonNull String title,
            boolean desktopMode,
            int listIndex) {
        mSessionHandle = sessionHandle;
        mUrl = url;
        mTitle = title;
        mDesktopMode = desktopMode;
        mListIndex = listIndex;
    }

    @NonNull
    public String getSessionHandle() {
        return mSessionHandle;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    public boolean isDesktopMode() {
        return mDesktopMode;
    }

    public int getListIndex() {
        return mListIndex;
    }
}
