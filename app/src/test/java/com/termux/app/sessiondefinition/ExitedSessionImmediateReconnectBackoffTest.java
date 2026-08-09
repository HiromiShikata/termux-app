package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class ExitedSessionImmediateReconnectBackoffTest {

    private static final String SESSION_NAME = "agent-one";

    private static final long START_MILLIS = 1_000_000L;

    @Test
    public void aSessionWhoseShellHasJustExitedIsReconnectedWithoutWaiting() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();

        Assert.assertTrue("a session that has not been reconnected before must be reconnected the moment"
                + " its shell exits, otherwise it stays dead until the next background scan",
            backoff.isReadyToReconnectImmediately(SESSION_NAME, START_MILLIS));
    }

    @Test
    public void aSessionThatDiesAgainImmediatelyIsLeftToTheBackgroundScan() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_MILLIS);

        Assert.assertFalse("a command that fails the instant it starts would reconnect in a tight loop"
                + " unless the repeat is made to wait",
            backoff.isReadyToReconnectImmediately(SESSION_NAME, START_MILLIS + 1_000L));
    }

    @Test
    public void aSessionThatDiesAgainAfterTheFirstWaitIsReconnectedImmediatelyAgain() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_MILLIS);

        Assert.assertTrue("once the wait after a single failed reconnect has elapsed the session must be"
                + " reconnected at once again rather than waiting for the scan",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                START_MILLIS + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void theWaitDoublesForEachConsecutiveReconnectAndNeverExceedsTheLongestWait() {
        Assert.assertEquals("the first repeat must wait the shortest wait",
            ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS,
            ExitedSessionImmediateReconnectBackoff.waitMillis(1));
        Assert.assertEquals("the second repeat must wait twice the shortest wait",
            ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS * 2,
            ExitedSessionImmediateReconnectBackoff.waitMillis(2));
        Assert.assertEquals("a session that keeps dying must settle at the longest wait rather than"
                + " growing without bound",
            ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS,
            ExitedSessionImmediateReconnectBackoff.waitMillis(50));
    }

    @Test
    public void aSessionThatStayedConnectedLongerThanTheLongestWaitIsTreatedAsAFreshFailure() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        long now = START_MILLIS;
        for (int repeat = 0; repeat < 8; repeat++) {
            backoff.recordImmediateReconnect(SESSION_NAME, now);
            now += ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS;
        }
        long afterStayingConnected = now + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS + 1L;
        backoff.recordImmediateReconnect(SESSION_NAME, afterStayingConnected);

        Assert.assertTrue("a session that ran for longer than the longest wait before dying again is not"
                + " failing in a loop, so its next exit must be reconnected after the shortest wait rather"
                + " than the accumulated one",
            backoff.isReadyToReconnectImmediately(SESSION_NAME,
                afterStayingConnected + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void aSessionThatNoLongerExistsIsForgotten() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        backoff.recordImmediateReconnect(SESSION_NAME, START_MILLIS);
        backoff.forgetSessionsOtherThan(new HashSet<>(Collections.singletonList("another-session")));

        Assert.assertTrue("a session that was removed and later recreated under the same name must not"
                + " inherit the wait recorded against the session that no longer exists",
            backoff.isReadyToReconnectImmediately(SESSION_NAME, START_MILLIS + 1L));
    }
}
