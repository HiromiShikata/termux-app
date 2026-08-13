package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

public class ExitedSessionWaitDoesNotCollapseAtItsCeilingTest {

    private static final String SESSION_THAT_KEEPS_DYING = "https://example.test/keeps-dying-on-start";

    private static final long START_TIME_MILLIS = 1_000_000L;

    private static final long SCAN_JITTER_MILLIS = 30L;

    private static long driveTheWaitUpToItsCeiling(ExitedSessionImmediateReconnectBackoff backoff) {
        long nowMillis = START_TIME_MILLIS;
        backoff.recordImmediateReconnect(SESSION_THAT_KEEPS_DYING, nowMillis);
        for (int precedingReconnects = 1;
             ExitedSessionImmediateReconnectBackoff.waitMillis(precedingReconnects)
                 < ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS;
             precedingReconnects++) {
            nowMillis += ExitedSessionImmediateReconnectBackoff.waitMillis(precedingReconnects);
            backoff.recordImmediateReconnect(SESSION_THAT_KEEPS_DYING, nowMillis);
        }
        return nowMillis;
    }

    @Test
    public void theWaitReachesItsCeilingBeforeTheCollapseIsTested() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        long atTheCeiling = driveTheWaitUpToItsCeiling(backoff);

        Assert.assertFalse("the session must be held by the longest wait at this point, otherwise the"
                + " collapse this class is about is not what the next test observes",
            backoff.isReadyToReconnectImmediately(SESSION_THAT_KEEPS_DYING,
                atTheCeiling + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS - 1L));
    }

    @Test
    public void aSessionStillDyingAtTheCeilingKeepsTheLongestWaitInsteadOfFallingBackToTheShortest() {
        ExitedSessionImmediateReconnectBackoff backoff = new ExitedSessionImmediateReconnectBackoff();
        long atTheCeiling = driveTheWaitUpToItsCeiling(backoff);

        long nextReconnect = atTheCeiling
            + ExitedSessionImmediateReconnectBackoff.LONGEST_WAIT_MILLIS + SCAN_JITTER_MILLIS;
        backoff.recordImmediateReconnect(SESSION_THAT_KEEPS_DYING, nextReconnect);

        Assert.assertFalse("a session that was never seen running must keep the longest wait, because the"
                + " time since its last reconnect is the wait this class imposed rather than evidence the"
                + " session stayed up",
            backoff.isReadyToReconnectImmediately(SESSION_THAT_KEEPS_DYING,
                nextReconnect + ExitedSessionImmediateReconnectBackoff.SHORTEST_WAIT_MILLIS));
    }
}
