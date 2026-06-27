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

    @NonNull
    public static SessionTimesLine of(@Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis,
                                      int subagentCount,
                                      long nowMillis) {
        String text = "call: " + relativeAgeOrMoreThanOneDay(callTimeMillis, nowMillis)
            + "  out: " + relativeAgeOrMoreThanOneDay(outTimeMillis, nowMillis)
            + "  reply: " + relativeAgeOrMoreThanOneDay(replyTimeMillis, nowMillis)
            + "  sub: " + subagentCount;
        return new SessionTimesLine(true, text);
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
}
