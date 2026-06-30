package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionReconnectingIndicatorState {

    static final long RECONNECTING_INDICATOR_MAX_DURATION_MILLIS = 30_000L;

    private SessionReconnectingIndicatorState() {
    }

    public static boolean shouldShowReconnectingIndicator(@NonNull String sessionName,
                                                          @Nullable SessionNewActivityStore store,
                                                          long nowMillis) {
        if (store == null) {
            return false;
        }
        if (!store.isReconnecting(sessionName)) {
            return false;
        }
        long startTimeMillis = store.getReconnectingStartTimeMillis(sessionName);
        return nowMillis - startTimeMillis <= RECONNECTING_INDICATOR_MAX_DURATION_MILLIS;
    }
}
