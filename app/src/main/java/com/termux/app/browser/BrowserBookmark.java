package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserBookmark {

    private final String mUrl;

    private final String mTitle;

    public BrowserBookmark(@NonNull String url, @NonNull String title) {
        mUrl = url;
        mTitle = title;
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
