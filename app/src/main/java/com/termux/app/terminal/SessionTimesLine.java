package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionTimesLine {

    private static final String ABSENT_VALUE = "-";

    private final boolean mVisible;

    @NonNull
    private final String mText;

    private SessionTimesLine(boolean visible, @NonNull String text) {
        mVisible = visible;
        mText = text;
    }

    @NonNull
    public static SessionTimesLine of(@Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis,
                                      long nowMillis) {
        if (callTimeMillis == null && outTimeMillis == null && replyTimeMillis == null) {
            return new SessionTimesLine(false, "");
        }
        String text = "call: " + relativeAgeOrAbsent(callTimeMillis, nowMillis)
            + "  out: " + relativeAgeOrAbsent(outTimeMillis, nowMillis)
            + "  reply: " + relativeAgeOrAbsent(replyTimeMillis, nowMillis);
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
    private static String relativeAgeOrAbsent(@Nullable Long timeMillis, long nowMillis) {
        return timeMillis == null ? ABSENT_VALUE
            : SessionNewActivityStore.formatRelativeAge(timeMillis, nowMillis);
    }
}
