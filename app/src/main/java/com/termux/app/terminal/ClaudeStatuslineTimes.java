package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClaudeStatuslineTimes {

    private static final Pattern CALL_PATTERN = tokenPattern("call");
    private static final Pattern OUT_PATTERN = tokenPattern("out");
    private static final Pattern REPLY_PATTERN = tokenPattern("reply");

    @Nullable
    private final Long mCallTimeMillis;

    @Nullable
    private final Long mOutTimeMillis;

    @Nullable
    private final Long mReplyTimeMillis;

    private ClaudeStatuslineTimes(@Nullable Long callTimeMillis,
                                  @Nullable Long outTimeMillis,
                                  @Nullable Long replyTimeMillis) {
        mCallTimeMillis = callTimeMillis;
        mOutTimeMillis = outTimeMillis;
        mReplyTimeMillis = replyTimeMillis;
    }

    @NonNull
    public static ClaudeStatuslineTimes parse(@Nullable String screenText, long nowMillis,
                                              @NonNull TimeZone timeZone) {
        if (screenText == null || screenText.isEmpty()) {
            return new ClaudeStatuslineTimes(null, null, null);
        }
        return new ClaudeStatuslineTimes(
            absoluteTimeMillis(CALL_PATTERN, screenText, nowMillis, timeZone),
            absoluteTimeMillis(OUT_PATTERN, screenText, nowMillis, timeZone),
            absoluteTimeMillis(REPLY_PATTERN, screenText, nowMillis, timeZone));
    }

    public boolean hasAnyToken() {
        return mCallTimeMillis != null || mOutTimeMillis != null || mReplyTimeMillis != null;
    }

    @Nullable
    public Long getCallTimeMillis() {
        return mCallTimeMillis;
    }

    @Nullable
    public Long getOutTimeMillis() {
        return mOutTimeMillis;
    }

    @Nullable
    public Long getReplyTimeMillis() {
        return mReplyTimeMillis;
    }

    @NonNull
    private static Pattern tokenPattern(@NonNull String name) {
        return Pattern.compile("\\b" + name + ":(\\d{1,2}):(\\d{2}):(\\d{2})\\b");
    }

    @Nullable
    private static Long absoluteTimeMillis(@NonNull Pattern pattern, @NonNull String screenText,
                                           long nowMillis, @NonNull TimeZone timeZone) {
        Matcher matcher = pattern.matcher(screenText);
        Long lastMatch = null;
        while (matcher.find()) {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = Integer.parseInt(matcher.group(3));
            if (hours > 23 || minutes > 59 || seconds > 59) {
                continue;
            }
            lastMatch = todayAt(hours, minutes, seconds, nowMillis, timeZone);
        }
        return lastMatch;
    }

    private static long todayAt(int hours, int minutes, int seconds, long nowMillis,
                                @NonNull TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
