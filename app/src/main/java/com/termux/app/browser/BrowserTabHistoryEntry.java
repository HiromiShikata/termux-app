package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserTabHistoryEntry {

    private final String mUrl;

    private final String mTitle;

    public BrowserTabHistoryEntry(@NonNull String url, @NonNull String title) {
        mUrl = url;
        mTitle = title.isEmpty() ? url : title;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }
}
