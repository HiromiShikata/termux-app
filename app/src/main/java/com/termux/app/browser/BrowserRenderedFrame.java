package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserRenderedFrame {

    public static final BrowserRenderedFrame BLANK = new BrowserRenderedFrame(null, null);

    private final BrowserTab mTab;

    private final String mCommittedUrl;

    private BrowserRenderedFrame(@Nullable BrowserTab tab, @Nullable String committedUrl) {
        this.mTab = tab;
        this.mCommittedUrl = committedUrl;
    }

    @NonNull
    public static BrowserRenderedFrame renderingTab(@NonNull BrowserTab tab) {
        return new BrowserRenderedFrame(tab, null);
    }

    @Nullable
    public BrowserTab getTab() {
        return mTab;
    }

    @Nullable
    public String getOwnerSessionHandle() {
        return mTab == null ? null : mTab.getSessionHandle();
    }

    @Nullable
    public String getCommittedUrl() {
        return mCommittedUrl;
    }

    public boolean isBlank() {
        return mTab == null;
    }

    public boolean isDisplaying(@Nullable BrowserTab tab) {
        return tab != null && tab == mTab;
    }

    @NonNull
    public BrowserRenderedFrame withCommittedUrl(@Nullable String committedUrl) {
        return new BrowserRenderedFrame(mTab, committedUrl);
    }
}
