package com.termux.view.url;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class DetectedUrlSanitizer {

    private static final String[] HINT_MARKERS = {
        "Jump to bottom",
        "Jump%20to%20bottom",
        "(ctrl+End)",
        "(ctrl%2BEnd)",
        "↓",
        "%E2%86%93",
    };

    private DetectedUrlSanitizer() {}

    @Nullable
    public static String sanitize(@Nullable String candidate) {
        if (candidate == null) return null;
        if (containsHintMarker(candidate)) return null;

        String truncated = truncateAtFirstWhitespace(candidate);
        String trimmed = trimTrailingArtifacts(truncated);

        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    private static boolean containsHintMarker(String value) {
        String lowerValue = value.toLowerCase(Locale.ROOT);
        for (String marker : HINT_MARKERS) {
            if (lowerValue.contains(marker.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String truncateAtFirstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return value.substring(0, index);
            }
        }
        return value;
    }

    private static String trimTrailingArtifacts(String value) {
        String result = value;
        while (result.length() > 0) {
            char character = result.charAt(result.length() - 1);
            if (Character.isWhitespace(character) || isTrailingPunctuation(character)) {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }
        return result;
    }

    private static boolean isTrailingPunctuation(char character) {
        switch (character) {
            case '.':
            case ',':
            case ';':
            case ':':
            case '!':
            case '?':
            case '\'':
            case '"':
                return true;
            default:
                return false;
        }
    }
}
