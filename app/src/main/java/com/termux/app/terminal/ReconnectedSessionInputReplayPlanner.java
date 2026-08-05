package com.termux.app.terminal;

/**
 * The bounded retry schedule for replaying the owner's typed input into a session that was just
 * reconnected in place. The reconnected session is not running for a short moment after it is
 * created, and the remote pseudo-terminal that the session reaches over ssh starts with ICRNL
 * enabled, which turns the payload's terminating carriage return into a line feed. A line feed
 * inserts a blank line into a remote TUI input box instead of submitting the message, so the
 * payload is written only once the remote terminal reports the raw input mode in which a carriage
 * return submits. Each attempt is a non-blocking message posted to the main Looper, so the window
 * ({@link #MAX_RETRY_ATTEMPTS} x {@link #RETRY_DELAY_MILLIS}) covers an ssh connection and a tmux
 * attach without blocking the UI thread for any of it.
 */
final class ReconnectedSessionInputReplayPlanner {

    static final int MAX_RETRY_ATTEMPTS = 100;

    static final long RETRY_DELAY_MILLIS = 100L;

    private ReconnectedSessionInputReplayPlanner() {
    }

    static boolean shouldScheduleAnotherAttempt(int remainingAttempts) {
        return remainingAttempts > 0;
    }

    static boolean hasReplayableInput(String pendingInput) {
        return pendingInput != null && !pendingInput.isEmpty();
    }

    static String replayPayload(String pendingInput) {
        return pendingInput + "\r";
    }

    static boolean shouldWriteNow(boolean localSessionRunning, boolean remoteTerminalSubmitsCarriageReturn) {
        return localSessionRunning && remoteTerminalSubmitsCarriageReturn;
    }

    static long maxReplayWindowMillis() {
        return (long) MAX_RETRY_ATTEMPTS * RETRY_DELAY_MILLIS;
    }
}
