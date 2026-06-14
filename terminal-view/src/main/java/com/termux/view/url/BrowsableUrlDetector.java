package com.termux.view.url;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class BrowsableUrlDetector {

    private BrowsableUrlDetector() {}

    public static boolean isLikelyBrowsableUrl(@Nullable String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return false;
        if (containsWhitespace(trimmed)) return false;
        String lowerCased = trimmed.toLowerCase(Locale.ROOT);
        if (!lowerCased.startsWith("http://") && !lowerCased.startsWith("https://")) return false;
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            return host != null && !host.isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return true;
        }
        return false;
    }
}
