package com.termux.app.browser;

import androidx.annotation.Nullable;

import com.termux.view.url.BrowsableUrlDetector;

public final class SessionNameBrowserTabUrlResolver {

    @Nullable
    public String resolve(@Nullable String sessionName) {
        if (sessionName == null) return null;
        String trimmedSessionName = sessionName.trim();
        if (trimmedSessionName.isEmpty()) return null;
        if (!BrowsableUrlDetector.isLikelyBrowsableUrl(trimmedSessionName)) return null;
        return trimmedSessionName;
    }
}
