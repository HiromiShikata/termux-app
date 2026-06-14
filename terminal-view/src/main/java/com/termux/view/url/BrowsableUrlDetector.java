package com.termux.view.url;

import android.net.Uri;
import android.util.Patterns;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class BrowsableUrlDetector {

    private BrowsableUrlDetector() {}

    public static boolean isLikelyBrowsableUrl(@Nullable String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return false;
        if (containsWhitespace(trimmed)) return false;
        if (Patterns.WEB_URL.matcher(trimmed).matches()) return true;
        return isHttpUrlWithHost(trimmed);
    }

    private static boolean isHttpUrlWithHost(String trimmed) {
        String lowerCased = trimmed.toLowerCase(Locale.ROOT);
        if (!lowerCased.startsWith("http://") && !lowerCased.startsWith("https://")) return false;
        String host = Uri.parse(trimmed).getHost();
        return host != null && !host.isEmpty();
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return true;
        }
        return false;
    }
}
