package com.termux.app.browser;

import android.util.Patterns;

import androidx.annotation.Nullable;

public final class SessionNameBrowserTabUrlResolver {

    @Nullable
    public String resolve(@Nullable String sessionName) {
        if (sessionName == null) return null;
        String trimmedSessionName = sessionName.trim();
        if (trimmedSessionName.isEmpty()) return null;
        if (!Patterns.WEB_URL.matcher(trimmedSessionName).matches()) return null;
        return trimmedSessionName;
    }
}
