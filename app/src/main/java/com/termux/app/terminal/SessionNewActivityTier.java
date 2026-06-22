package com.termux.app.terminal;

import androidx.annotation.Nullable;

public enum SessionNewActivityTier {

    NONE,
    YELLOW,
    RED;

    public static SessionNewActivityTier resolve(@Nullable Long lastOutputActivityTimeMillis,
                                                 @Nullable Long lastExplicitCallTimeMillis,
                                                 @Nullable Long lastSeenTimeMillis) {
        if (isPending(lastExplicitCallTimeMillis, lastSeenTimeMillis)) {
            return RED;
        }
        if (isPending(lastOutputActivityTimeMillis, lastSeenTimeMillis)) {
            return YELLOW;
        }
        return NONE;
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
