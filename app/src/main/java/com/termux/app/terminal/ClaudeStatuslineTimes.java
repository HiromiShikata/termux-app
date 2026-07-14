package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClaudeStatuslineTimes {

    private static final Pattern CALL_DATED_PATTERN = datedTokenPattern("call");
    private static final Pattern OUT_DATED_PATTERN = datedTokenPattern("out");
    private static final Pattern REPLY_DATED_PATTERN = datedTokenPattern("reply");
    private static final Pattern CALL_PATTERN = tokenPattern("call");
    private static final Pattern OUT_PATTERN = tokenPattern("out");
    private static final Pattern REPLY_PATTERN = tokenPattern("reply");
    private static final Pattern SUBAGENT_COUNT_PATTERN = Pattern.compile("\\bSUB:(\\d+)\\b");

    @Nullable
    private final Long mCallTimeMillis;

    @Nullable
    private final Long mOutTimeMillis;

    @Nullable
    private final Long mReplyTimeMillis;

    private final int mSubagentCount;

    private ClaudeStatuslineTimes(@Nullable Long callTimeMillis,
                                  @Nullable Long outTimeMillis,
                                  @Nullable Long replyTimeMillis,
                                  int subagentCount) {
        mCallTimeMillis = callTimeMillis;
        mOutTimeMillis = outTimeMillis;
        mReplyTimeMillis = replyTimeMillis;
        mSubagentCount = subagentCount;
    }

    @NonNull
    public static ClaudeStatuslineTimes parse(@Nullable String screenText, long nowMillis,
                                              @NonNull TimeZone timeZone) {
        if (screenText == null || screenText.isEmpty()) {
            return new ClaudeStatuslineTimes(null, null, null, 0);
        }
        return new ClaudeStatuslineTimes(
            absoluteTimeMillis(CALL_DATED_PATTERN, CALL_PATTERN, screenText, nowMillis, timeZone),
            absoluteTimeMillis(OUT_DATED_PATTERN, OUT_PATTERN, screenText, nowMillis, timeZone),
            absoluteTimeMillis(REPLY_DATED_PATTERN, REPLY_PATTERN, screenText, nowMillis, timeZone),
            subagentCount(screenText));
    }

    public boolean hasAnyToken() {
        return mCallTimeMillis != null || mOutTimeMillis != null || mReplyTimeMillis != null
            || mSubagentCount > 0;
    }

    public int getSubagentCount() {
        return mSubagentCount;
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

    private static int subagentCount(@NonNull String screenText) {
        Matcher matcher = SUBAGENT_COUNT_PATTERN.matcher(screenText);
        int lastMatch = 0;
        while (matcher.find()) {
            try {
                lastMatch = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException overflow) {
                lastMatch = Integer.MAX_VALUE;
            }
        }
        return lastMatch;
    }

    @NonNull
    private static Pattern tokenPattern(@NonNull String name) {
        return Pattern.compile("\\b" + name + ":(\\d{1,2}):(\\d{2}):(\\d{2})\\b");
    }

    @NonNull
    private static Pattern datedTokenPattern(@NonNull String name) {
        // YYYY-MM-DD, then a 'T' or space separator, then HH:MM:SS, then an optional
        // fractional-seconds part and an optional 'Z' or +HH:MM / -HH:MM zone offset.
        return Pattern.compile("\\b" + name
            + ":(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2}):(\\d{2})(?:\\.\\d+)?"
            + "(Z|[+-]\\d{2}:\\d{2})?");
    }

    @Nullable
    private static Long absoluteTimeMillis(@NonNull Pattern datedPattern, @NonNull Pattern pattern,
                                           @NonNull String screenText, long nowMillis,
                                           @NonNull TimeZone timeZone) {
        Long datedMatch = datedTimeMillis(datedPattern, screenText, timeZone);
        if (datedMatch != null) {
            return datedMatch;
        }
        return clockTimeMillis(pattern, screenText, nowMillis, timeZone);
    }

    @Nullable
    private static Long datedTimeMillis(@NonNull Pattern datedPattern, @NonNull String screenText,
                                        @NonNull TimeZone timeZone) {
        Matcher matcher = datedPattern.matcher(screenText);
        Long lastMatch = null;
        while (matcher.find()) {
            Long parsed = epochMillisFromDatedMatch(matcher, timeZone);
            if (parsed != null) {
                lastMatch = parsed;
            }
        }
        return lastMatch;
    }

    @Nullable
    private static Long epochMillisFromDatedMatch(@NonNull Matcher matcher,
                                                  @NonNull TimeZone timeZone) {
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hours = Integer.parseInt(matcher.group(4));
        int minutes = Integer.parseInt(matcher.group(5));
        int seconds = Integer.parseInt(matcher.group(6));
        String zone = matcher.group(7);
        try {
            LocalDateTime localDateTime =
                LocalDateTime.of(year, month, day, hours, minutes, seconds);
            if (zone == null) {
                return localDateTime.atZone(timeZone.toZoneId()).toInstant().toEpochMilli();
            }
            ZoneOffset offset = "Z".equals(zone) ? ZoneOffset.UTC : ZoneOffset.of(zone);
            return OffsetDateTime.of(localDateTime, offset).toInstant().toEpochMilli();
        } catch (DateTimeException invalidDateTime) {
            return null;
        }
    }

    @Nullable
    private static Long clockTimeMillis(@NonNull Pattern pattern, @NonNull String screenText,
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
            lastMatch = mostRecentClockTimeAtOrBefore(hours, minutes, seconds, nowMillis, timeZone);
        }
        return lastMatch;
    }

    private static long mostRecentClockTimeAtOrBefore(int hours, int minutes, int seconds,
                                                      long nowMillis, @NonNull TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() > nowMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        return calendar.getTimeInMillis();
    }
}
