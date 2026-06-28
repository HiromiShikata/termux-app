package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ProjectFooterActionTabSelection {

    private ProjectFooterActionTabSelection() {
    }

    @Nullable
    public static BrowserTab resolveReusableTab(@NonNull BrowserTabManager tabManager,
                                                @NonNull String sessionHandle,
                                                @NonNull String targetUrl) {
        return tabManager.findTabByUrl(sessionHandle, targetUrl);
    }
}
