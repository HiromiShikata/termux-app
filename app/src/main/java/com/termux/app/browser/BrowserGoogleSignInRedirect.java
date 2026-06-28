package com.termux.app.browser;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public final class BrowserGoogleSignInRedirect {

    private static final List<String> EXTERNAL_BROWSER_HOSTS = Arrays.asList(
        "accounts.google.com",
        "docs.google.com",
        "sheets.google.com",
        "drive.google.com",
        "slides.google.com",
        "forms.google.com");

    private BrowserGoogleSignInRedirect() {
    }

    public static boolean requiresExternalBrowser(@Nullable String url) {
        return matchesHost(hostOf(url));
    }

    private static boolean matchesHost(@Nullable String host) {
        if (host == null) return false;
        for (String externalHost : EXTERNAL_BROWSER_HOSTS) {
            if (host.equals(externalHost)) return true;
        }
        return false;
    }

    @Nullable
    private static String hostOf(@Nullable String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        Uri uri = Uri.parse(trimmed);
        String scheme = uri.getScheme();
        if (scheme == null) return null;
        String lowerScheme = scheme.toLowerCase(java.util.Locale.ROOT);
        if (!lowerScheme.equals("http") && !lowerScheme.equals("https")) return null;
        String host = uri.getHost();
        if (host == null) return null;
        return host.toLowerCase(java.util.Locale.ROOT);
    }

    @NonNull
    public static List<String> externalBrowserHosts() {
        return EXTERNAL_BROWSER_HOSTS;
    }
}
