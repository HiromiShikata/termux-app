package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Paces the reconnects made the moment a session's shell process exits. Reconnecting at that moment is
 * what keeps a session connected instead of leaving it dead until the next background scan, but a
 * command that fails as soon as it starts exits again immediately and would be rebuilt in a tight loop.
 * Spacing the repeats leaves such a session to the background scan for the length of the wait while a
 * session that stayed connected longer than the longest wait is treated as a fresh failure rather than
 * carrying the wait accumulated by an earlier run.
 */
public final class ExitedSessionImmediateReconnectBackoff {

    static final long SHORTEST_WAIT_MILLIS = 10L * 1000L;

    static final long LONGEST_WAIT_MILLIS = 5L * 60L * 1000L;

    private static final class ExitAttempts {

        private int consecutiveReconnects;

        private long lastReconnectTimeMillis;
    }

    private final Map<String, ExitAttempts> mAttemptsBySessionName = new HashMap<>();

    public synchronized boolean isReadyToReconnectImmediately(@NonNull String sessionName, long nowMillis) {
        ExitAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null) {
            return true;
        }
        return nowMillis - attempts.lastReconnectTimeMillis >= waitMillis(attempts.consecutiveReconnects);
    }

    public synchronized void recordImmediateReconnect(@NonNull String sessionName, long nowMillis) {
        ExitAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null || stayedConnectedLongEnoughToBeAFreshFailure(attempts, nowMillis)) {
            attempts = new ExitAttempts();
            mAttemptsBySessionName.put(sessionName, attempts);
        }
        attempts.consecutiveReconnects++;
        attempts.lastReconnectTimeMillis = nowMillis;
    }

    public synchronized void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
        mAttemptsBySessionName.keySet().retainAll(sessionNamesToKeep);
    }

    private static boolean stayedConnectedLongEnoughToBeAFreshFailure(@NonNull ExitAttempts attempts,
                                                                      long nowMillis) {
        return nowMillis - attempts.lastReconnectTimeMillis > LONGEST_WAIT_MILLIS;
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
