package com.termux.app.terminal;

import com.termux.app.outputtag.OutputTagScanner;

import java.util.List;

public final class CallToUserTagScanner {

    private final OutputTagScanner outputTagScanner =
        new OutputTagScanner("call-to-user", CallToUserTagScanner::normalizeReason);

    public static List<String> extractReasons(String output) {
        return new OutputTagScanner("call-to-user", CallToUserTagScanner::normalizeReason).extractValues(output);
    }

    public static String normalizeReason(String innerText) {
        if (innerText == null) return null;
        String collapsed = collapseHorizontalWhitespace(innerText);
        String trimmed = collapsed.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed;
    }

    static String collapseHorizontalWhitespace(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousWasHorizontalWhitespace = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == ' ' || character == '\t') {
                previousWasHorizontalWhitespace = true;
                continue;
            }
            if (previousWasHorizontalWhitespace && character != '\n') {
                builder.append(' ');
            }
            previousWasHorizontalWhitespace = false;
            builder.append(character);
        }
        if (previousWasHorizontalWhitespace) {
            builder.append(' ');
        }
        return builder.toString();
    }

    public List<String> newReasons(String output) {
        return outputTagScanner.newValues(output);
    }
}
