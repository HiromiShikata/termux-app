package com.termux.view.url;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class DetectedUrlSanitizer {

    private static final String JUMP_TO_BOTTOM_HINT = "Jump to bottom (ctrl+End)";

    private static final String DOWN_ARROW = "↓";

    private static final String DOWN_ARROW_PERCENT_ENCODED = "%E2%86%93";

    private static final String[] HINT_MARKERS = {
        "Jump to bottom",
        "Jump%20to%20bottom",
        "(ctrl+End)",
        "(ctrl%2BEnd)",
        DOWN_ARROW,
        DOWN_ARROW_PERCENT_ENCODED,
    };

    private DetectedUrlSanitizer() {}

    @Nullable
    public static String sanitize(@Nullable String candidate) {
        if (candidate == null) return null;

        String truncated = truncateAtFirstSpaceOrHintMarker(candidate);
        String withoutHint = removeHintChrome(truncated);
        String trimmed = trimTrailingArtifacts(withoutHint);

        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    private static String truncateAtFirstSpaceOrHintMarker(String value) {
        int end = value.length();

        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                end = index;
                break;
            }
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);
        for (String marker : HINT_MARKERS) {
            int markerIndex = lowerValue.indexOf(marker.toLowerCase(Locale.ROOT));
            if (markerIndex >= 0 && markerIndex < end) {
                end = markerIndex;
            }
        }

        return value.substring(0, end);
    }

    private static String removeHintChrome(String value) {
        String result = value.replace(JUMP_TO_BOTTOM_HINT, "");
        result = result.replace(DOWN_ARROW, "");
        result = replaceIgnoreCase(result, DOWN_ARROW_PERCENT_ENCODED);
        return result;
    }

    private static String replaceIgnoreCase(String value, String target) {
        StringBuilder builder = new StringBuilder();
        String lowerValue = value.toLowerCase(Locale.ROOT);
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        int index = 0;
        while (index < value.length()) {
            int matchIndex = lowerValue.indexOf(lowerTarget, index);
            if (matchIndex < 0) {
                builder.append(value, index, value.length());
                break;
            }
            builder.append(value, index, matchIndex);
            index = matchIndex + target.length();
        }
        return builder.toString();
    }

    private static String trimTrailingArtifacts(String value) {
        String result = value;
        boolean changed = true;
        while (changed) {
            changed = false;
            while (result.length() > 0) {
                char character = result.charAt(result.length() - 1);
                if (Character.isWhitespace(character) || isTrailingPunctuation(character)) {
                    result = result.substring(0, result.length() - 1);
                    changed = true;
                } else {
                    break;
                }
            }
            if (result.toLowerCase(Locale.ROOT).endsWith("%20")) {
                result = result.substring(0, result.length() - 3);
                changed = true;
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
