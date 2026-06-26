package com.termux.app.browser;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public final class BrowserTab {

    public static final String DEFAULT_URL = "https://www.google.com";

    private final String mId = UUID.randomUUID().toString();

    private final String mSessionHandle;

    private String mUrl;

    private String mTitle;

    private BrowserViewMode mViewMode = BrowserViewMode.DESKTOP;

    private Bitmap mFavicon;

    public BrowserTab(@NonNull String sessionHandle, @NonNull String url) {
        this.mSessionHandle = sessionHandle;
        this.mUrl = url;
        this.mTitle = url;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public String getSessionHandle() {
        return mSessionHandle;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@Nullable String url) {
        if (url != null && !url.isEmpty()) this.mUrl = url;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    public void setTitle(@Nullable String title) {
        this.mTitle = (title == null || title.isEmpty()) ? mUrl : title;
    }

    @NonNull
    public BrowserViewMode getViewMode() {
        return mViewMode;
    }

    public void setViewMode(@NonNull BrowserViewMode viewMode) {
        this.mViewMode = viewMode;
    }

    public boolean isDesktopMode() {
        return mViewMode.isDesktop();
    }

    public void setDesktopMode(boolean desktopMode) {
        this.mViewMode = desktopMode ? BrowserViewMode.DESKTOP : BrowserViewMode.MOBILE;
    }

    @Nullable
    public Bitmap getFavicon() {
        return mFavicon;
    }

    public void setFavicon(@Nullable Bitmap favicon) {
        this.mFavicon = favicon;
    }
}
