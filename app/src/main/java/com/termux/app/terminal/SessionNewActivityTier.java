package com.termux.app.terminal;

import androidx.annotation.Nullable;

public enum SessionNewActivityTier {

    NONE,
    YELLOW,
    RED;

    static final long YELLOW_MIN_AWAY_MILLIS = 30_000L;

    static final long YELLOW_MAX_AGE_MILLIS = 10L * 60L * 1000L;

    public static SessionNewActivityTier resolve(@Nullable Long lastOutputActivityTimeMillis,
                                                 @Nullable Long lastExplicitCallTimeMillis,
                                                 @Nullable Long lastSeenTimeMillis) {
        if (isPending(lastExplicitCallTimeMillis, lastSeenTimeMillis, 0L, null)) {
            return RED;
        }
        if (isPending(lastOutputActivityTimeMillis, lastSeenTimeMillis, 0L, null)) {
            return YELLOW;
        }
        return NONE;
    }

    public static SessionNewActivityTier resolve(@Nullable Long lastOutputActivityTimeMillis,
                                                 @Nullable Long lastExplicitCallTimeMillis,
                                                 @Nullable Long lastSeenTimeMillis,
                                                 long nowMillis) {
        if (isPending(lastExplicitCallTimeMillis, lastSeenTimeMillis, 0L, nowMillis)) {
            return RED;
        }
        if (isPending(lastOutputActivityTimeMillis, lastSeenTimeMillis, YELLOW_MIN_AWAY_MILLIS, nowMillis)
            && isWithinRecencyWindow(lastOutputActivityTimeMillis, nowMillis)) {
            return YELLOW;
        }
        return NONE;
    }

    private static boolean isWithinRecencyWindow(@Nullable Long lastOutputActivityTimeMillis, long nowMillis) {
        if (lastOutputActivityTimeMillis == null) {
            return false;
        }
        return nowMillis - lastOutputActivityTimeMillis <= YELLOW_MAX_AGE_MILLIS;
    }

    private static boolean isPending(@Nullable Long signalTimeMillis, @Nullable Long lastSeenTimeMillis,
                                     long minAwayMillis, @Nullable Long nowMillis) {
        if (signalTimeMillis == null) {
            return false;
        }
        if (lastSeenTimeMillis == null) {
            return true;
        }
        if (signalTimeMillis <= lastSeenTimeMillis) {
            return false;
        }
        if (minAwayMillis <= 0L || nowMillis == null) {
            return true;
        }
        return nowMillis - lastSeenTimeMillis >= minAwayMillis;
    }
}
