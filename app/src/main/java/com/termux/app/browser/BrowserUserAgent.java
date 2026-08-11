package com.termux.app.browser;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BrowserUserAgent {

    private static final Pattern ENGINE_MAJOR_VERSION = Pattern.compile("Chrome/(\\d+)\\.");

    private static final String DESKTOP_USER_AGENT_PREFIX =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/";

    private static final String DESKTOP_USER_AGENT_SUFFIX = ".0.0.0 Safari/537.36";

    private BrowserUserAgent() {
    }

    @Nullable
    public static String engineMajorVersion(@Nullable String engineUserAgent) {
        if (engineUserAgent == null) {
            return null;
        }
        Matcher matcher = ENGINE_MAJOR_VERSION.matcher(engineUserAgent);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Nullable
    public static String desktopUserAgentFrom(@Nullable String engineUserAgent) {
        String engineMajorVersion = engineMajorVersion(engineUserAgent);
        if (engineMajorVersion == null) {
            return null;
        }
        return DESKTOP_USER_AGENT_PREFIX + engineMajorVersion + DESKTOP_USER_AGENT_SUFFIX;
    }
}
