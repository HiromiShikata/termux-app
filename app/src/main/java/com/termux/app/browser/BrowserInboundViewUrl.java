package com.termux.app.browser;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BrowserInboundViewUrl {

    private static final String ACTION_VIEW = "android.intent.action.VIEW";

    private static final String MEET_HOST = "meet.google.com";

    private BrowserInboundViewUrl() {
    }

    @Nullable
    public static String resolveInAppBrowserUrl(@Nullable String action, @Nullable String dataString) {
        if (action == null || !action.equals(ACTION_VIEW)) return null;
        if (dataString == null) return null;
        String trimmed = dataString.trim();
        if (trimmed.isEmpty()) return null;

        Uri uri = Uri.parse(trimmed);
        String scheme = uri.getScheme();
        if (scheme == null) return null;
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) return null;

        String host = uri.getHost();
        if (host == null) return null;
        host = host.toLowerCase(Locale.ROOT);
        if (!host.equals(MEET_HOST)) return null;

        return trimmed;
    }
}
