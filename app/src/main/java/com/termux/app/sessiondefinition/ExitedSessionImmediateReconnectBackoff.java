package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.Set;

public final class ExitedSessionImmediateReconnectBackoff {

    static final long SHORTEST_WAIT_MILLIS = 10L * 1000L;

    static final long LONGEST_WAIT_MILLIS = 5L * 60L * 1000L;

    public synchronized boolean isReadyToReconnectImmediately(@NonNull String sessionName, long nowMillis) {
        return false;
    }

    public synchronized void recordImmediateReconnect(@NonNull String sessionName, long nowMillis) {
    }

    public synchronized void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
    }

    static long waitMillis(int consecutiveReconnects) {
        return 0L;
    }
}
