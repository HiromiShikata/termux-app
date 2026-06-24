package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserPersistedTab {

    private final String mUrl;

    private final String mTitle;

    private final boolean mDesktopMode;

    public BrowserPersistedTab(@NonNull String url, @NonNull String title, boolean desktopMode) {
        mUrl = url;
        mTitle = title;
        mDesktopMode = desktopMode;
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
}
