package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ReconnectedSessionInputReplayPlannerTest {

    @Test
    public void replayWindowIsBoundedToHalfASecondSoTheUiDoesNotFreezeForSeconds() {
        Assert.assertEquals(500L, ReconnectedSessionInputReplayPlanner.maxReplayWindowMillis());
        Assert.assertTrue(ReconnectedSessionInputReplayPlanner.maxReplayWindowMillis() <= 1_000L);
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
    public void retryCountIsBoundedShortRatherThanTheOldTwoAndAHalfSecondSchedule() {
        Assert.assertEquals(10, ReconnectedSessionInputReplayPlanner.MAX_RETRY_ATTEMPTS);
        Assert.assertEquals(50L, ReconnectedSessionInputReplayPlanner.RETRY_DELAY_MILLIS);
    }
}
