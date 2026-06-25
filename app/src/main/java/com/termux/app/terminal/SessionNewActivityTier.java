package com.termux.app.terminal;

import androidx.annotation.Nullable;

public enum SessionNewActivityTier {

    NONE,
    GRAY,
    YELLOW,
    RED;

    static final long YELLOW_MAX_AGE_MILLIS = 10L * 60L * 1000L;

    public static SessionNewActivityTier resolve(@Nullable Long lastOutputActivityTimeMillis,
                                                 @Nullable Long lastExplicitCallTimeMillis,
                                                 @Nullable Long lastUserInputTimeMillis,
                                                 @Nullable Long lastSeenTimeMillis) {
        if (isExplicitCallUnanswered(lastExplicitCallTimeMillis, lastUserInputTimeMillis)) {
            return RED;
        }
        if (isPending(lastOutputActivityTimeMillis, lastSeenTimeMillis)) {
            return YELLOW;
        }
        return NONE;
    }

    public static SessionNewActivityTier resolve(@Nullable Long lastOutputActivityTimeMillis,
                                                 @Nullable Long lastExplicitCallTimeMillis,
                                                 @Nullable Long lastUserInputTimeMillis,
                                                 @Nullable Long lastSeenTimeMillis,
                                                 long nowMillis) {
        if (isExplicitCallUnanswered(lastExplicitCallTimeMillis, lastUserInputTimeMillis)) {
            return RED;
        }
        if (lastOutputActivityTimeMillis == null) {
            return NONE;
        }
        if (nowMillis - lastOutputActivityTimeMillis <= YELLOW_MAX_AGE_MILLIS) {
            return YELLOW;
        }
        return GRAY;
    }

    private static boolean isExplicitCallUnanswered(@Nullable Long lastExplicitCallTimeMillis,
                                                    @Nullable Long lastUserInputTimeMillis) {
        if (lastExplicitCallTimeMillis == null) {
            return false;
        }
        return lastUserInputTimeMillis == null || lastExplicitCallTimeMillis > lastUserInputTimeMillis;
    }

    private static boolean isPending(@Nullable Long signalTimeMillis, @Nullable Long lastSeenTimeMillis) {
        if (signalTimeMillis == null) {
            return false;
        }
        if (lastSeenTimeMillis == null) {
            return true;
        }
        return signalTimeMillis > lastSeenTimeMillis;
    }
}
