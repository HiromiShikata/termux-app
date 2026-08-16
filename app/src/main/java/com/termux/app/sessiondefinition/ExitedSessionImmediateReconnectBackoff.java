package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ExitedSessionImmediateReconnectBackoff {

    static final long SHORTEST_WAIT_MILLIS = 10L * 1000L;

    static final long LONGEST_WAIT_MILLIS = 5L * 60L * 1000L;

    private static final class DeathAttempts {
        private int consecutiveReconnects;
        private long lastReconnectTimeMillis;
        private boolean seenRunningLongEnoughAfterTheReconnect;
    }

    private final Map<String, DeathAttempts> mAttemptsBySessionName = new HashMap<>();

    public synchronized boolean isReadyToReconnectImmediately(@NonNull String sessionName, long nowMillis) {
        DeathAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null) {
            return true;
        }
        return nowMillis - attempts.lastReconnectTimeMillis >= waitMillis(attempts.consecutiveReconnects);
    }

    public synchronized long millisUntilReadyToReconnectImmediately(@NonNull String sessionName,
                                                                     long nowMillis) {
        DeathAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null) {
            return 0L;
        }
        long remainingMillis = attempts.lastReconnectTimeMillis
            + waitMillis(attempts.consecutiveReconnects) - nowMillis;
        return Math.max(0L, remainingMillis);
    }

    public synchronized void recordImmediateReconnect(@NonNull String sessionName, long nowMillis) {
        DeathAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null || attempts.seenRunningLongEnoughAfterTheReconnect) {
            attempts = new DeathAttempts();
            mAttemptsBySessionName.put(sessionName, attempts);
        }
        attempts.consecutiveReconnects++;
        attempts.lastReconnectTimeMillis = nowMillis;
    }

    public synchronized void recordObservedRunning(@NonNull String sessionName, long nowMillis) {
        DeathAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null) {
            return;
        }
        if (nowMillis - attempts.lastReconnectTimeMillis > SHORTEST_WAIT_MILLIS) {
            attempts.seenRunningLongEnoughAfterTheReconnect = true;
        }
    }

    public synchronized void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
        mAttemptsBySessionName.keySet().retainAll(sessionNamesToKeep);
    }

    static long waitMillis(int consecutiveReconnects) {
        if (consecutiveReconnects <= 0) {
            return 0L;
        }
        long waitMillis = SHORTEST_WAIT_MILLIS;
        for (int doubling = 1; doubling < consecutiveReconnects; doubling++) {
            if (waitMillis >= LONGEST_WAIT_MILLIS) {
                break;
            }
            waitMillis *= 2;
        }
        return Math.min(waitMillis, LONGEST_WAIT_MILLIS);
    }
}
