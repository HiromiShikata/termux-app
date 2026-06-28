package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionTimesLine {

    private final boolean mVisible;

    @NonNull
    private final String mText;

    private SessionTimesLine(boolean visible, @NonNull String text) {
        mVisible = visible;
        mText = text;
    }

    @NonNull
    public static SessionTimesLine hidden() {
        return new SessionTimesLine(false, "");
    }

    static final int TIME_VALUE_COLUMN_WIDTH = 3;
    static final int SUBAGENT_COUNT_COLUMN_WIDTH = 3;
    static final String REPLY_UNKNOWN_LABEL = "-";

    @NonNull
    public static SessionTimesLine of(@Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis,
                                      int subagentCount,
                                      long nowMillis) {
        String text = "call: " + relativeAgeOrMoreThanOneDay(callTimeMillis, nowMillis)
            + "  out: " + relativeAgeOrMoreThanOneDay(outTimeMillis, nowMillis)
            + "  reply: " + replyAgeOrUnknown(replyTimeMillis, nowMillis)
            + "  sub: " + subagentCount;
        return new SessionTimesLine(true, text);
    }

    @NonNull
    public static SessionTimesLine ofColumnAligned(@Nullable Long callTimeMillis,
                                                   @Nullable Long outTimeMillis,
                                                   @Nullable Long replyTimeMillis,
                                                   int subagentCount,
                                                   long nowMillis) {
        String text = "call: " + padColumn(relativeAgeOrMoreThanOneDay(callTimeMillis, nowMillis), TIME_VALUE_COLUMN_WIDTH)
            + " out: " + padColumn(relativeAgeOrMoreThanOneDay(outTimeMillis, nowMillis), TIME_VALUE_COLUMN_WIDTH)
            + " reply: " + padColumn(replyAgeOrUnknown(replyTimeMillis, nowMillis), TIME_VALUE_COLUMN_WIDTH)
            + " sub: " + padColumn(Integer.toString(subagentCount), SUBAGENT_COUNT_COLUMN_WIDTH);
        return new SessionTimesLine(true, text);
    }

    @NonNull
    static String padColumn(@NonNull String value, int columnWidth) {
        if (value.length() >= columnWidth) {
            return value;
        }
        StringBuilder padded = new StringBuilder(columnWidth);
        padded.append(value);
        while (padded.length() < columnWidth) {
            padded.append(' ');
        }
        return padded.toString();
    }

    public boolean isVisible() {
        return mVisible;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @NonNull
    private static String relativeAgeOrMoreThanOneDay(@Nullable Long timeMillis, long nowMillis) {
        return timeMillis == null ? SessionNewActivityStore.MORE_THAN_ONE_DAY_LABEL
            : SessionNewActivityStore.formatRelativeAge(timeMillis, nowMillis);
    }

    @NonNull
    private static String replyAgeOrUnknown(@Nullable Long replyTimeMillis, long nowMillis) {
        return replyTimeMillis == null ? REPLY_UNKNOWN_LABEL
            : SessionNewActivityStore.formatRelativeAge(replyTimeMillis, nowMillis);
    }
}
