package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class ExitedSessionImmediateReconnectBackoffTest {

    private static final String SESSION_NAME = "background-session";

    private static final long START_TIME_MILLIS = 1_000_000L;

    @Test
    public void aSessionThatHasNotBeenReconnectedYetIsReadyImmediately() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();

        assertTrue("the first death of a session is not a loop, so it must be reconnected at once",
            backoff.isReadyToReconnectImmediately(SESSION_NAME, START_TIME_MILLIS));
    }

    @Test
    public void aSessionThatDiesAgainInsideTheShortestWaitIsNotReadyYet() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS);

        assertFalse("a command that fails as soon as it starts would be rebuilt in a tight loop, so the"
                + " repeat must wait",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                START_TIME_MILLIS + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS - 1L));
    }

    @Test
    public void aSessionBecomesReadyAgainOnceTheShortestWaitHasElapsed() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS);

        assertTrue("the wait spaces the repeats rather than stopping them, so the session is reconnected"
                + " again once it elapses",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                START_TIME_MILLIS + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void eachConsecutiveReconnectDoublesTheWaitUpToTheLongestOne() {
        assertEquals(0L, ExitedSessionImmediateReconnectBackoff.waitMillis(0));
        assertEquals(ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS,
            ExitedSessionImmediateReconnectBackoff.waitMillis(1));
        assertEquals(ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS * 2,
            ExitedSessionImmediateReconnectBackoff.waitMillis(2));
        assertEquals("the wait must stop growing so a session that keeps dying is still retried by the"
                + " background scan rather than abandoned",
            ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS,
            ExitedSessionImmediateReconnectBackoff.waitMillis(20));
    }

    @Test
    public void aSessionThatStayedConnectedLongerThanTheLongestWaitStartsFromTheShortestWaitAgain() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS);
        backoff.recordObservedRunning(SESSION_NAME, START_TIME_MILLIS
            + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS + 1L);
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS
            + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS + 1L);

        assertTrue("a session seen running for longer than the longest wait failed freshly rather than"
                + " looping, so it must not carry the wait accumulated by an earlier run",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                START_TIME_MILLIS + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS + 1L
                    + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void aSessionSeenRunningOnlyBrieflyAfterItsReconnectKeepsTheWaitItAccumulated() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS);
        backoff.recordObservedRunning(SESSION_NAME, START_TIME_MILLIS
            + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS);
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS
            + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS + 1L);

        assertFalse("being seen running no longer than the shortest wait is the session dying again rather"
                + " than staying up, so the wait must keep growing",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                START_TIME_MILLIS + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS + 1L
                    + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void aSessionThatIsNoLongerPresentIsForgotten() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_TIME_MILLIS);
        backoff.forgetSessionsOtherThan(Collections.singleton("another-session"));

        assertTrue("a session the owner removed leaves no wait behind for a later session of the same name",
            backoff.isReadyToReconnectImmediately(SESSION_NAME, START_TIME_MILLIS));
    }
}
