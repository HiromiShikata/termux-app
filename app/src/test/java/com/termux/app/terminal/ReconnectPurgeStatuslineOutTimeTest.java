package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ReconnectPurgeStatuslineOutTimeTest {

    private static final String SESSION_NAME = "session-alpha";

    private static final long ELEVEN_MINUTES_MILLIS = 11L * 60L * 1000L;

    private final HungSessionDetector backgroundHungSessionDetector = new HungSessionDetector(
        TermuxTerminalSessionActivityClient.BACKGROUND_RECONNECT_STALE_OUT_MAX_AGE_MILLIS);

    @Test
    public void theReconnectPurgeMustNotHandTheStaleStatuslineOutputTimeToTheReplacementSession() {
        long nowMillis = System.currentTimeMillis();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, null, nowMillis - ELEVEN_MINUTES_MILLIS, null);

        assertTrue("the seeded session must start out judged hung, otherwise this test would not "
                + "exercise the reconnect purge at all",
            backgroundHungSessionDetector.isHung(store.getStatuslineOutTimeMillis(SESSION_NAME), nowMillis));

        store.purgeSessionPreservingStatuslineTimes(SESSION_NAME);

        assertNull("the reconnect purge tears down the old session and hands the same session name to a "
                + "replacement, so the recorded statusline output time of the torn-down session must not "
                + "survive into the replacement",
            store.getStatuslineOutTimeMillis(SESSION_NAME));
    }

    @Test
    public void aReplacementSessionMustNotStillBeJudgedHungImmediatelyAfterTheReconnectPurge() {
        long nowMillis = System.currentTimeMillis();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, null, nowMillis - ELEVEN_MINUTES_MILLIS, null);

        store.purgeSessionPreservingStatuslineTimes(SESSION_NAME);

        assertFalse("a session that has just been reconnected has had no opportunity to render a "
                + "statusline yet, so the inherited output time must not keep it judged hung and "
                + "requeue it for reconnect on the very next scan",
            backgroundHungSessionDetector.isHung(store.getStatuslineOutTimeMillis(SESSION_NAME), nowMillis));
    }
}
