package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class ExitedSessionWaitResetsWhenTheSessionOutlivesTheShortestWaitTest {

    private static final String SESSION_THE_OWNER_IS_WATCHING = "https://example.test/reload-button-is-red";

    private static final long START_TIME_MILLIS = 1_000_000L;

    private static long driveTheWaitUpToItsCeiling(ExitedSessionImmediateReconnectBackoff backoff) {
        long nowMillis = START_TIME_MILLIS;
        backoff.recordImmediateReconnect(SESSION_THE_OWNER_IS_WATCHING, nowMillis);
        for (int precedingReconnects = 1;
             ExitedSessionImmediateReconnectBackoff.waitMillis(precedingReconnects)
                 < ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS;
             precedingReconnects++) {
            nowMillis += ExitedSessionImmediateReconnectBackoff.waitMillis(precedingReconnects);
            backoff.recordImmediateReconnect(SESSION_THE_OWNER_IS_WATCHING, nowMillis);
        }
        return nowMillis;
    }

    @Test
    public void aSessionSeenRunningPastTheShortestWaitAfterItsReconnectStartsItsCountAgain() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        long atTheCeiling = driveTheWaitUpToItsCeiling(backoff);

        long seenRunningAt = atTheCeiling
            + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS + 1L;
        backoff.recordObservedRunning(SESSION_THE_OWNER_IS_WATCHING, seenRunningAt);
        backoff.recordImmediateReconnect(SESSION_THE_OWNER_IS_WATCHING, seenRunningAt);

        Assert.assertTrue("a session that came up and outlived the fastest rebuild this class ever"
                + " performs has shown that its reconnect worked, so it must not go on being withheld for"
                + " the five minutes the ceiling imposes on a session that never came up at all",
            backoff.isReadyToReconnectImmediately(SESSION_THE_OWNER_IS_WATCHING,
                seenRunningAt + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }

    @Test
    public void aSessionSeenRunningOnlyInsideTheShortestWaitAfterItsReconnectKeepsItsEscalation() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        long atTheCeiling = driveTheWaitUpToItsCeiling(backoff);

        long seenRunningAt = atTheCeiling
            + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS - 1L;
        backoff.recordObservedRunning(SESSION_THE_OWNER_IS_WATCHING, seenRunningAt);
        backoff.recordImmediateReconnect(SESSION_THE_OWNER_IS_WATCHING, seenRunningAt);

        Assert.assertFalse("a session caught alive for a moment inside the shortest wait is the tight"
                + " death loop this escalation exists for, so being seen at all must not hand it a fresh"
                + " ten second wait",
            backoff.isReadyToReconnectImmediately(SESSION_THE_OWNER_IS_WATCHING,
                seenRunningAt + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }
}
