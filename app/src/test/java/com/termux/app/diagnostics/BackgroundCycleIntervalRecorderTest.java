package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BackgroundCycleIntervalRecorderTest {

    private static final long SCHEDULED_INTERVAL_MILLIS = 60_000L;

    @Test
    public void theFirstCycleHasNoPredecessorSoItYieldsNoInterval() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder();

        recorder.recordCycle(1_000L, SCHEDULED_INTERVAL_MILLIS, false);

        Assert.assertEquals(1L, recorder.getCycleCount());
        Assert.assertTrue("a single cycle cannot describe a gap, and inventing one would read as"
            + " evidence that the cycle ran on time", recorder.getLongestIntervals().isEmpty());
    }

    @Test
    public void aSecondCycleRecordsTheWallClockGapSinceTheFirst() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder();

        recorder.recordCycle(1_000L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(62_000L, SCHEDULED_INTERVAL_MILLIS, false);

        List<BackgroundCycleInterval> intervals = recorder.getLongestIntervals();
        Assert.assertEquals(1, intervals.size());
        Assert.assertEquals(61_000L, intervals.get(0).getIntervalMillis());
        Assert.assertEquals(62_000L, intervals.get(0).getObservedAtMillis());
        Assert.assertEquals(SCHEDULED_INTERVAL_MILLIS, intervals.get(0).getScheduledIntervalMillis());
        Assert.assertFalse(intervals.get(0).isActivityVisible());
    }

    @Test
    public void theActivityVisibilityAtTheCycleIsCarriedSoABackgroundFreezeIsDistinguishable() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder();

        recorder.recordCycle(0L, SCHEDULED_INTERVAL_MILLIS, true);
        recorder.recordCycle(60_000L, SCHEDULED_INTERVAL_MILLIS, true);

        Assert.assertTrue(recorder.getLongestIntervals().get(0).isActivityVisible());
    }

    @Test
    public void aLongGapSurvivesLaterOrdinaryGapsSoItIsStillVisibleInAReportCapturedMuchLater() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder(2);

        recorder.recordCycle(0L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(10_800_000L, SCHEDULED_INTERVAL_MILLIS, false);
        for (long cycle = 1; cycle <= 20; cycle++) {
            recorder.recordCycle(10_800_000L + cycle * 60_000L, SCHEDULED_INTERVAL_MILLIS, true);
        }

        List<BackgroundCycleInterval> intervals = recorder.getLongestIntervals();
        Assert.assertEquals(2, intervals.size());
        Assert.assertEquals("the three hour freeze is the whole point of the measurement and must not be"
            + " displaced by the ordinary gaps that follow it", 10_800_000L,
            intervals.get(0).getIntervalMillis());
        Assert.assertEquals(60_000L, intervals.get(1).getIntervalMillis());
        Assert.assertEquals(22L, recorder.getCycleCount());
    }

    @Test
    public void gapsAreOrderedLongestFirst() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder();

        recorder.recordCycle(0L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(60_000L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(660_000L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(720_000L, SCHEDULED_INTERVAL_MILLIS, false);

        List<BackgroundCycleInterval> intervals = recorder.getLongestIntervals();
        Assert.assertEquals(600_000L, intervals.get(0).getIntervalMillis());
        Assert.assertEquals(60_000L, intervals.get(1).getIntervalMillis());
        Assert.assertEquals(60_000L, intervals.get(2).getIntervalMillis());
    }

    @Test
    public void aClockThatMovesBackwardsIsRecordedRatherThanDiscarded() {
        BackgroundCycleIntervalRecorder recorder = new BackgroundCycleIntervalRecorder();

        recorder.recordCycle(600_000L, SCHEDULED_INTERVAL_MILLIS, false);
        recorder.recordCycle(60_000L, SCHEDULED_INTERVAL_MILLIS, false);

        List<BackgroundCycleInterval> intervals = recorder.getLongestIntervals();
        Assert.assertEquals("silently dropping it would hide a clock change behind an apparently"
            + " healthy cycle history", 1, intervals.size());
        Assert.assertEquals(-540_000L, intervals.get(0).getIntervalMillis());
    }
}
