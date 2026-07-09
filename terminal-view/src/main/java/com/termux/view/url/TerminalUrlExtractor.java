package com.termux.view.url;

import androidx.annotation.Nullable;

public final class TerminalUrlExtractor {

    private TerminalUrlExtractor() {}

    @Nullable
    public static String extractBrowsableUrlAt(@Nullable String rowText, int column) {
        return extractBrowsableUrlAt(rowText, column, null);
    }

    @Nullable
    public static String extractBrowsableUrlAt(@Nullable String rowText, int column, @Nullable String hintScopeRowText) {
        if (rowText == null) return null;
        if (column < 0 || column >= rowText.length()) return null;
        if (Character.isWhitespace(rowText.charAt(column))) return null;

        int start = column;
        while (start > 0 && !Character.isWhitespace(rowText.charAt(start - 1))) start--;

        int end = column;
        int lastIndex = rowText.length() - 1;
        while (end < lastIndex && !Character.isWhitespace(rowText.charAt(end + 1))) end++;

        String token = rowText.substring(start, end + 1);
        String sanitized = DetectedUrlSanitizer.sanitize(token, hintScopeRowText);
        if (sanitized == null) return null;
        return BrowsableUrlDetector.isLikelyBrowsableUrl(sanitized) ? sanitized : null;
    }
}
