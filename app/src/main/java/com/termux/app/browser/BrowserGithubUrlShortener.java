package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BrowserGithubUrlShortener {

    private static final String GITHUB_HOST = "github.com";

    private static final String WWW_GITHUB_HOST = "www.github.com";

    private BrowserGithubUrlShortener() {
    }

    @NonNull
    public static String shorten(@Nullable String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return "";

        String withoutScheme = stripScheme(trimmed);
        int pathStart = withoutScheme.indexOf('/');
        String authority = pathStart < 0 ? withoutScheme : withoutScheme.substring(0, pathStart);
        String pathAndBeyond = pathStart < 0 ? "" : withoutScheme.substring(pathStart);

        if (!isGithubHost(hostOf(authority))) return trimmed;

        String path = stripTrailingSlashes(stripLeadingSlashes(pathAndBeyond));
        if (path.isEmpty()) return GITHUB_HOST;
        return path;
    }

    @NonNull
    private static String stripScheme(@NonNull String url) {
        int schemeSeparator = url.indexOf("://");
        if (schemeSeparator < 0) return url;
        return url.substring(schemeSeparator + 3);
    }

    @NonNull
    private static String hostOf(@NonNull String authority) {
        String host = authority;
        int userInfoIndex = host.indexOf('@');
        if (userInfoIndex >= 0) host = host.substring(userInfoIndex + 1);
        int portIndex = host.indexOf(':');
        if (portIndex >= 0) host = host.substring(0, portIndex);
        return host;
    }

    private static boolean isGithubHost(@NonNull String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        return lowerHost.equals(GITHUB_HOST) || lowerHost.equals(WWW_GITHUB_HOST);
    }

    @NonNull
    private static String stripLeadingSlashes(@NonNull String value) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') start++;
        return value.substring(start);
    }

    @NonNull
    private static String stripTrailingSlashes(@NonNull String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
