package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Paces the repeated reconnects of a session that is judged unresponsive because it has produced no
 * output for a long time. That judgement is made from the age of the session's last recorded output,
 * and reconnecting the session does not make that recording any newer, so a session that is alive and
 * simply quiet answers the judgement identically on the following scan and is reconnected again for as
 * long as it stays quiet. Spacing the repeats keeps such a session eligible for a later attempt without
 * rebuilding its working connection once per scan forever. A session whose shell process has exited is
 * a different failure, is identified without reference to its output age, and is not paced by this.
 */
public final class HungSessionReconnectBackoff {

    static final long SHORTEST_WAIT_MILLIS = 10L * 60L * 1000L;

    static final long LONGEST_WAIT_MILLIS = 60L * 60L * 1000L;

    private static final class SilenceAttempts {

        @Nullable
        private final Long lastOutTimeMillis;

        private int consecutiveAttempts;

        private long lastAttemptTimeMillis;

        private SilenceAttempts(@Nullable Long lastOutTimeMillis) {
            this.lastOutTimeMillis = lastOutTimeMillis;
        }

        private boolean describesTheSameSilenceAs(@Nullable Long otherLastOutTimeMillis) {
            if (lastOutTimeMillis == null) {
                return otherLastOutTimeMillis == null;
            }
            return lastOutTimeMillis.equals(otherLastOutTimeMillis);
        }
    }

    private final Map<String, SilenceAttempts> mAttemptsBySessionName = new HashMap<>();

    public synchronized boolean isReadyToAttemptAgain(@NonNull String sessionName,
                                                      @Nullable Long lastOutTimeMillis,
                                                      long nowMillis) {
        SilenceAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null || !attempts.describesTheSameSilenceAs(lastOutTimeMillis)) {
            return true;
        }
        return nowMillis - attempts.lastAttemptTimeMillis >= waitMillis(attempts.consecutiveAttempts);
    }

    public synchronized void recordAttemptForSilence(@NonNull String sessionName,
                                                     @Nullable Long lastOutTimeMillis,
                                                     long nowMillis) {
        SilenceAttempts attempts = mAttemptsBySessionName.get(sessionName);
        if (attempts == null || !attempts.describesTheSameSilenceAs(lastOutTimeMillis)) {
            attempts = new SilenceAttempts(lastOutTimeMillis);
            mAttemptsBySessionName.put(sessionName, attempts);
        }
        attempts.consecutiveAttempts++;
        attempts.lastAttemptTimeMillis = nowMillis;
    }

    public synchronized void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
        mAttemptsBySessionName.keySet().retainAll(sessionNamesToKeep);
    }

    static long waitMillis(int consecutiveAttempts) {
        if (consecutiveAttempts <= 0) {
            return 0L;
        }
        long waitMillis = SHORTEST_WAIT_MILLIS;
        for (int doubling = 1; doubling < consecutiveAttempts; doubling++) {
            if (waitMillis >= LONGEST_WAIT_MILLIS) {
                break;
            }
            waitMillis *= 2;
        }
        return Math.min(waitMillis, LONGEST_WAIT_MILLIS);
    }
}
