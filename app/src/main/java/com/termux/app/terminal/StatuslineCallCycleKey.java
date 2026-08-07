package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatuslineCallCycleKey {

    private static final Pattern CALL_TOKEN_PATTERN = tokenPattern("call");
    private static final Pattern OUT_TOKEN_PATTERN = tokenPattern("out");
    private static final Pattern REPLY_TOKEN_PATTERN = tokenPattern("reply");

    private static final String TOKEN_SEPARATOR = " ";

    private StatuslineCallCycleKey() {
    }

    @NonNull
    public static String resolve(@NonNull String value, @Nullable String output,
                                 int occurrenceStartIndex) {
        String tokens = statuslineTokensBefore(output, occurrenceStartIndex);
        if (tokens.isEmpty()) {
            return value;
        }
        return tokens + TOKEN_SEPARATOR + value;
    }

    @NonNull
    private static String statuslineTokensBefore(@Nullable String output, int occurrenceStartIndex) {
        if (output == null || occurrenceStartIndex <= 0) {
            return "";
        }
        int regionEnd = Math.min(occurrenceStartIndex, output.length());
        String callToken = lastTokenIn(CALL_TOKEN_PATTERN, output, regionEnd);
        String outToken = lastTokenIn(OUT_TOKEN_PATTERN, output, regionEnd);
        String replyToken = lastTokenIn(REPLY_TOKEN_PATTERN, output, regionEnd);
        if (callToken.isEmpty() && outToken.isEmpty() && replyToken.isEmpty()) {
            return "";
        }
        return callToken + TOKEN_SEPARATOR + outToken + TOKEN_SEPARATOR + replyToken;
    }

    @NonNull
    private static String lastTokenIn(@NonNull Pattern pattern, @NonNull String output,
                                      int regionEnd) {
        Matcher matcher = pattern.matcher(output);
        matcher.region(0, regionEnd);
        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group();
        }
        return lastMatch;
    }

    @NonNull
    private static Pattern tokenPattern(@NonNull String name) {
        String clock = "(?:\\d{1,2}):(?:\\d{2}):(?:\\d{2})";
        String date = "(?:\\d{4})-(?:\\d{2})-(?:\\d{2})[T ]";
        String optionalFractionalSeconds = "(?:\\.\\d+)?";
        String optionalZoneOffset = "(?:Z|[+-]\\d{2}:\\d{2})?";
        return Pattern.compile("\\b" + name + ":(?:" + date + ")?" + clock
            + optionalFractionalSeconds + optionalZoneOffset);
    }
}
