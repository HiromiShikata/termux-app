package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class MainThreadStallSampleScheduleTest {

    private static final long STALL_THRESHOLD_MILLIS = 80L;

    private static final long POLL_INTERVAL_MILLIS = 20L;

    private final MainThreadStallSampleSchedule mSchedule =
        new MainThreadStallSampleSchedule(STALL_THRESHOLD_MILLIS, POLL_INTERVAL_MILLIS);

    @Test
    public void theFirstAttemptIsScheduledForTheMomentAStallBecomesRecordable() {
        Assert.assertEquals("waking before the threshold spends a wake-up on an attempt the recorder is"
                + " certain to reject, and the stall can end before the next one",
            STALL_THRESHOLD_MILLIS, mSchedule.sleepMillisAfterAttemptAt(0L));
    }

    @Test
    public void anAttemptMadePartWayToTheThresholdWaitsOnlyForTheRemainder() {
        Assert.assertEquals(STALL_THRESHOLD_MILLIS - 30L, mSchedule.sleepMillisAfterAttemptAt(30L));
        Assert.assertEquals(1L, mSchedule.sleepMillisAfterAttemptAt(STALL_THRESHOLD_MILLIS - 1L));
    }

    @Test
    public void onceTheThresholdHasPassedTheScheduleFallsBackToThePollInterval() {
        Assert.assertEquals(POLL_INTERVAL_MILLIS,
            mSchedule.sleepMillisAfterAttemptAt(STALL_THRESHOLD_MILLIS));
        Assert.assertEquals(POLL_INTERVAL_MILLIS, mSchedule.sleepMillisAfterAttemptAt(500L));
    }

    @Test
    public void aSleepIsNeverZeroSoTheWatchdogThreadCannotSpin() {
        for (long elapsedMillis = 0L; elapsedMillis <= 400L; elapsedMillis++) {
            Assert.assertTrue("a zero or negative sleep turns the watchdog into a busy loop that competes"
                    + " with the main thread it is measuring, at elapsed " + elapsedMillis,
                mSchedule.sleepMillisAfterAttemptAt(elapsedMillis) > 0L);
        }
    }
}
