package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserRenderedFrameOwnership {

    private BrowserRenderedFrameOwnership() {
    }

    public static boolean isRenderedFrameOwnedByCurrentSession(@Nullable String currentSessionHandle,
                                                               @Nullable String renderedFrameOwnerHandle) {
        return currentSessionHandle != null
            && currentSessionHandle.equals(renderedFrameOwnerHandle);
    }

    public static boolean isRenderedFrameForeign(@Nullable String currentSessionHandle,
                                                 @Nullable String renderedFrameOwnerHandle) {
        return renderedFrameOwnerHandle != null
            && !renderedFrameOwnerHandle.equals(currentSessionHandle);
    }

    public static boolean requiresCoverForFrame(@Nullable String currentSessionHandle,
                                                @Nullable String renderedFrameOwnerHandle,
                                                @Nullable String currentlyLoadedUrl,
                                                @NonNull String targetUrl,
                                                boolean browserWillBeVisible) {
        if (!browserWillBeVisible) return false;
        if (isRenderedFrameForeign(currentSessionHandle, renderedFrameOwnerHandle)) return true;
        return BrowserPageTransition.requiresCoverWhileLoading(
            currentlyLoadedUrl, targetUrl, browserWillBeVisible);
    }
}
