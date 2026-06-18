package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserDisplayedTabResolution {

    public enum Action {
        CLEAR,
        KEEP,
        LOAD
    }

    private final Action mAction;

    private BrowserDisplayedTabResolution(@NonNull Action action) {
        this.mAction = action;
    }

    @NonNull
    public Action getAction() {
        return mAction;
    }

    public boolean shouldDisplay() {
        return mAction != Action.CLEAR;
    }

    public boolean shouldLoadWebView() {
        return mAction == Action.LOAD;
    }

    @NonNull
    public static BrowserDisplayedTabResolution resolve(@Nullable String currentSessionHandle,
                                                        @Nullable String activeTabSessionHandle,
                                                        @Nullable String webViewOwnerHandle,
                                                        @Nullable String displayedTabSessionHandle,
                                                        boolean forceReload) {
        if (currentSessionHandle == null || activeTabSessionHandle == null
                || !currentSessionHandle.equals(activeTabSessionHandle)) {
            return new BrowserDisplayedTabResolution(Action.CLEAR);
        }
        boolean webViewOwnedByCurrentSession = currentSessionHandle.equals(webViewOwnerHandle);
        boolean displayedTabBelongsToCurrentSession = currentSessionHandle.equals(displayedTabSessionHandle);
        if (forceReload || !webViewOwnedByCurrentSession || !displayedTabBelongsToCurrentSession) {
            return new BrowserDisplayedTabResolution(Action.LOAD);
        }
        return new BrowserDisplayedTabResolution(Action.KEEP);
    }
}
