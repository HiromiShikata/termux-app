package com.termux.app.terminal;

/**
 * The bounded retry schedule for replaying the owner's typed input into a session that was just
 * reconnected in place. The reconnected session is not running for a short moment after it is
 * created, so the input is replayed once it reports running. Each attempt is a non-blocking message
 * posted to the main Looper; the bound keeps the total replay window short ({@link
 * #MAX_RETRY_ATTEMPTS} x {@link #RETRY_DELAY_MILLIS}) so switching to a finished session never
 * freezes the UI for seconds while waiting for the new session to come up.
 */
final class ReconnectedSessionInputReplayPlanner {

    static final int MAX_RETRY_ATTEMPTS = 10;

    static final long RETRY_DELAY_MILLIS = 50L;

    private ReconnectedSessionInputReplayPlanner() {
    }

    static boolean shouldScheduleAnotherAttempt(int remainingAttempts) {
        return remainingAttempts > 0;
    }

    static long maxReplayWindowMillis() {
        return (long) MAX_RETRY_ATTEMPTS * RETRY_DELAY_MILLIS;
    }
}
