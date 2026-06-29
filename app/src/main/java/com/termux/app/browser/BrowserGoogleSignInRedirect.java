package com.termux.app.browser;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BrowserGoogleSignInRedirect {

    private static final List<String> EXTERNAL_BROWSER_HOSTS = Arrays.asList(
        "accounts.google.com",
        "docs.google.com",
        "sheets.google.com",
        "drive.google.com",
        "slides.google.com",
        "forms.google.com");

    private static final List<String> AUTH_HOSTS = Arrays.asList(
        "oauth2.googleapis.com",
        "accounts.youtube.com",
        "myaccount.google.com",
        "gds.google.com");

    private static final List<String> AUTH_HOST_SUFFIXES = Arrays.asList(
        ".accounts.google.com",
        ".oauth2.googleapis.com");

    private static final String GOOGLE_ACCOUNTS_REGIONAL_PREFIX = "accounts.google.";

    private static final List<String> AUTH_PATH_FRAGMENTS = Arrays.asList(
        "/o/oauth2/",
        "/signin/",
        "/webauthn",
        "/passkey",
        "/securitykey",
        "/challenge/");

    private BrowserGoogleSignInRedirect() {
    }

    public static boolean requiresExternalBrowser(@Nullable String url) {
        Uri uri = parseHttpUri(url);
        if (uri == null) return false;
        String host = lowerCase(uri.getHost());
        if (host == null) return false;
        if (matchesDocumentHost(host)) return true;
        if (matchesAuthHost(host)) return true;
        return matchesGoogleAuthPath(host, lowerCase(uri.getPath()));
    }

    private static boolean matchesDocumentHost(@NonNull String host) {
        return EXTERNAL_BROWSER_HOSTS.contains(host);
    }

    private static boolean matchesAuthHost(@NonNull String host) {
        if (AUTH_HOSTS.contains(host)) return true;
        for (String suffix : AUTH_HOST_SUFFIXES) {
            if (host.endsWith(suffix)) return true;
        }
        return matchesRegionalGoogleAccountsHost(host);
    }

    private static boolean matchesRegionalGoogleAccountsHost(@NonNull String host) {
        if (!host.startsWith(GOOGLE_ACCOUNTS_REGIONAL_PREFIX)) return false;
        String topLevel = host.substring(GOOGLE_ACCOUNTS_REGIONAL_PREFIX.length());
        return isTopLevelDomainSuffix(topLevel);
    }

    private static boolean isTopLevelDomainSuffix(@NonNull String topLevel) {
        if (topLevel.isEmpty()) return false;
        String[] labels = topLevel.split("\\.", -1);
        if (labels.length < 1 || labels.length > 2) return false;
        for (String label : labels) {
            if (!isTopLevelDomainLabel(label)) return false;
        }
        return true;
    }

    private static boolean isTopLevelDomainLabel(@NonNull String label) {
        if (label.length() < 2 || label.length() > 3) return false;
        for (int index = 0; index < label.length(); index++) {
            if (!Character.isLetter(label.charAt(index))) return false;
        }
        return true;
    }

    private static boolean matchesGoogleAuthPath(@NonNull String host, @Nullable String path) {
        if (path == null || !isGoogleHost(host)) return false;
        for (String fragment : AUTH_PATH_FRAGMENTS) {
            if (path.contains(fragment)) return true;
        }
        return false;
    }

    private static boolean isGoogleHost(@NonNull String host) {
        return host.equals("google.com") || host.endsWith(".google.com");
    }

    @Nullable
    private static Uri parseHttpUri(@Nullable String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        Uri uri = Uri.parse(trimmed);
        String scheme = lowerCase(uri.getScheme());
        if (scheme == null) return null;
        if (!scheme.equals("http") && !scheme.equals("https")) return null;
        return uri;
    }

    @Nullable
    private static String lowerCase(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    @NonNull
    public static List<String> externalBrowserHosts() {
        return EXTERNAL_BROWSER_HOSTS;
    }
}
