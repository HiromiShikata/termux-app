package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserWebViewNavigation {

    private final BrowserTab mTab;

    private final String mSessionHandle;

    private BrowserWebViewNavigation(@NonNull BrowserTab tab) {
        this.mTab = tab;
        this.mSessionHandle = tab.getSessionHandle();
    }

    @NonNull
    public static BrowserWebViewNavigation startedBy(@NonNull BrowserTab tab) {
        return new BrowserWebViewNavigation(tab);
    }

    @NonNull
    public BrowserTab getTab() {
        return mTab;
    }

    @NonNull
    public String getSessionHandle() {
        return mSessionHandle;
    }

    @Nullable
    public BrowserTab tabForUrlCallback(@Nullable String currentSessionHandle,
                                        @Nullable String webViewOwnerHandle,
                                        @Nullable String callbackUrl,
                                        @Nullable String webViewCurrentUrl) {
        if (!ownsCurrentWebView(currentSessionHandle, webViewOwnerHandle)) return null;
        if (callbackUrl == null || !callbackUrl.equals(webViewCurrentUrl)) return null;
        return mTab;
    }

    @Nullable
    public BrowserTab tabForTitleCallback(@Nullable String currentSessionHandle,
                                          @Nullable String webViewOwnerHandle,
                                          @Nullable String webViewCurrentUrl) {
        if (!ownsCurrentWebView(currentSessionHandle, webViewOwnerHandle)) return null;
        if (webViewCurrentUrl == null || !webViewCurrentUrl.equals(mTab.getUrl())) return null;
        return mTab;
    }

    private boolean ownsCurrentWebView(@Nullable String currentSessionHandle,
                                       @Nullable String webViewOwnerHandle) {
        if (currentSessionHandle == null) return false;
        if (!currentSessionHandle.equals(mSessionHandle)) return false;
        return currentSessionHandle.equals(webViewOwnerHandle);
    }
}
