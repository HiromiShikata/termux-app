package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserNewTabEntry {

    private final String mUrl;

    private final String mTitle;

    private final boolean mBookmark;

    public BrowserNewTabEntry(@NonNull String url, @NonNull String title, boolean bookmark) {
        mUrl = url;
        mTitle = title.isEmpty() ? url : title;
        mBookmark = bookmark;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    public boolean isBookmark() {
        return mBookmark;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BrowserNewTabEntry)) return false;
        BrowserNewTabEntry that = (BrowserNewTabEntry) other;
        return mBookmark == that.mBookmark
            && mUrl.equals(that.mUrl)
            && mTitle.equals(that.mTitle);
    }

    @Override
    public int hashCode() {
        int result = mUrl.hashCode();
        result = 31 * result + mTitle.hashCode();
        result = 31 * result + (mBookmark ? 1 : 0);
        return result;
    }
}
