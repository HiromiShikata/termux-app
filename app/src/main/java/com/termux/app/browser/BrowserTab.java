package com.termux.app.browser;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public final class BrowserTab {

    public static final String DEFAULT_URL = "https://www.google.com";

    private final String mId = UUID.randomUUID().toString();

    private final String mSessionHandle;

    private String mUrl;

    private String mTitle;

    private boolean mDesktopMode = true;

    private Bundle mSavedWebViewState;

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

    public boolean isDesktopMode() {
        return mDesktopMode;
    }

    public void setDesktopMode(boolean desktopMode) {
        this.mDesktopMode = desktopMode;
    }

    public boolean hasSavedWebViewState() {
        return mSavedWebViewState != null;
    }

    @Nullable
    public Bundle getSavedWebViewState() {
        return mSavedWebViewState;
    }

    public void setSavedWebViewState(@Nullable Bundle savedWebViewState) {
        this.mSavedWebViewState = savedWebViewState;
    }
}
