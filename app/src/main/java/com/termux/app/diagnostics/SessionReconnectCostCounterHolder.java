package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

/**
 * Process-lifetime holder for the counter that measures the main-thread time spent reconnecting dead
 * sessions, so the diagnostics report can attribute a bulk reconnect that the stall watchdog's
 * threshold is too coarse to record.
 */
public final class SessionReconnectCostCounterHolder {

    private static final SessionReconnectCostCounter INSTANCE = new SessionReconnectCostCounter();

    private SessionReconnectCostCounterHolder() {
    }

    @NonNull
    public static SessionReconnectCostCounter getInstance() {
        return INSTANCE;
    }
}
