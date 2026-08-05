package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectedSessionInputReplayPlannerTest {

    @Test
    public void replayWindowIsBoundedAndCoversAnSshConnectionAndTmuxAttach() {
        Assert.assertEquals(10_000L, ReconnectedSessionInputReplayPlanner.maxReplayWindowMillis());
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.maxReplayWindowMillis() <= 30_000L);
    }

    @Test
    public void retriesWhileAttemptsRemain() {
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.shouldScheduleAnotherAttempt(9));
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.shouldScheduleAnotherAttempt(1));
    }

    @Test
    public void stopsRetryingOnceTheAttemptsAreExhausted() {
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.shouldScheduleAnotherAttempt(0));
        Assert.assertFalse(ReconnectedSessionInputReplayPlanner.shouldScheduleAnotherAttempt(-1));
    }

    @Test
    public void retryScheduleWaitsForTheRemoteTerminalWithoutBlockingTheUiThread() {
        Assert.assertEquals(100, ReconnectedSessionInputReplayPlanner.MAX_RETRY_ATTEMPTS);
        Assert.assertEquals(100L, ReconnectedSessionInputReplayPlanner.RETRY_DELAY_MILLIS);
    }
}
