package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class MainThreadStallRecorderTest {

    private static final long STALL_THRESHOLD_MILLIS = 250L;

    private static StackTraceElement[] stackNaming(String methodName) {
        return new StackTraceElement[]{
            new StackTraceElement("com.termux.app.Blocking", methodName, "Blocking.java", 42),
            new StackTraceElement("android.os.Looper", "loop", "Looper.java", 223),
        };
    }

    private void stall(MainThreadStallRecorder recorder, long postedAtMillis, long blockedMillis,
                       String methodName) {
        recorder.heartbeatPosted(postedAtMillis);
        recorder.sampleWhileOutstanding(postedAtMillis + STALL_THRESHOLD_MILLIS, stackNaming(methodName));
        recorder.heartbeatRan(postedAtMillis + blockedMillis);
    }

    @Test
    public void aCodePathThatBlockedManyShortTimesOutranksOneThatBlockedOnceForLonger() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        stall(recorder, 1000L, 900L, "recreateActivity");
        stall(recorder, 3000L, 400L, "scrollTerminal");
        stall(recorder, 5000L, 400L, "scrollTerminal");
        stall(recorder, 7000L, 400L, "scrollTerminal");

        java.util.List<MainThreadStallHotPath> hotPaths = recorder.getHotPathsByTotalBlockedMillis();

        Assert.assertEquals("each distinct stack must become one hot path", 2, hotPaths.size());
        Assert.assertTrue("the path that took the most total time must rank first. Actual: "
                + hotPaths.get(0).getStackTrace(),
            hotPaths.get(0).getStackTrace().contains("com.termux.app.Blocking.scrollTerminal"));
        Assert.assertEquals("repeated stalls on one path must accumulate",
            1200L, hotPaths.get(0).getTotalBlockedMillis());
        Assert.assertEquals(3L, hotPaths.get(0).getStallCount());
        Assert.assertEquals("the worst single occurrence must stay visible",
            400L, hotPaths.get(0).getMaxBlockedMillis());
    }

    @Test
    public void aStallIsSampledAgainOnlyAfterItHasBlockedForAnotherThreshold() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);

        Assert.assertFalse("before the threshold there is nothing worth the cost of sampling",
            recorder.needsStackSample(1100L));
        Assert.assertTrue("past the threshold the blocking stack must be captured",
            recorder.needsStackSample(1300L));

        recorder.sampleWhileOutstanding(1300L, stackNaming("reflowBuffer"));

        Assert.assertFalse("sampling again within one threshold only adds load to an already blocked main"
                + " thread",
            recorder.needsStackSample(1500L));
        Assert.assertTrue("a stall that goes on blocking for another threshold must be sampled again,"
                + " otherwise everything the main thread does after the first sample is credited to it",
            recorder.needsStackSample(1550L));
    }

    @Test
    public void aHeavyPathFoundLateStillDisplacesTheNoiseThatFilledTheTable() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        for (int noiseIndex = 0; noiseIndex < 200; noiseIndex++) {
            stall(recorder, 1000L * noiseIndex, 300L, "noise" + noiseIndex);
        }
        stall(recorder, 900000L, 5000L, "scrollTerminal");

        java.util.List<MainThreadStallHotPath> hotPaths = recorder.getHotPathsByTotalBlockedMillis();

        Assert.assertTrue("the path that blocked the main thread the longest must survive the bound. Actual: "
                + hotPaths.get(0).getStackTrace(),
            hotPaths.get(0).getStackTrace().contains("com.termux.app.Blocking.scrollTerminal"));
    }

    @Test
    public void aHeartbeatInsideTheThresholdContributesToNoHotPath() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);
        recorder.heartbeatRan(1100L);

        Assert.assertTrue("a frame the main thread answered in time is not a blocking path",
            recorder.getHotPathsByTotalBlockedMillis().isEmpty());
    }

    @Test
    public void aHeartbeatThatRunsWithinTheThresholdRecordsNoStall() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);
        recorder.heartbeatRan(1100L);

        Assert.assertEquals("a heartbeat the main thread answered inside the threshold is not a stall",
            0L, recorder.getStallCount());
        Assert.assertEquals(0L, recorder.getMaxStallMillis());
        Assert.assertEquals("", recorder.getMaxStallStackTrace());
    }

    @Test
    public void aHeartbeatThatOutlivesTheThresholdRecordsTheStallWithTheSampledStack() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);
        recorder.sampleWhileOutstanding(1400L, stackNaming("reflowBuffer"));
        recorder.heartbeatRan(1500L);

        Assert.assertEquals(1L, recorder.getStallCount());
        Assert.assertEquals("the stall lasts until the main thread answers the heartbeat",
            500L, recorder.getMaxStallMillis());
        Assert.assertTrue("the recorded stack must name the code the main thread was running. Actual: "
                + recorder.getMaxStallStackTrace(),
            recorder.getMaxStallStackTrace().contains("com.termux.app.Blocking.reflowBuffer"));
    }

    @Test
    public void theLongestStallKeepsItsOwnStackWhenAShorterStallFollows() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);
        recorder.sampleWhileOutstanding(1400L, stackNaming("reflowBuffer"));
        recorder.heartbeatRan(2000L);
        recorder.heartbeatPosted(3000L);
        recorder.sampleWhileOutstanding(3400L, stackNaming("scanOutput"));
        recorder.heartbeatRan(3500L);

        Assert.assertEquals(2L, recorder.getStallCount());
        Assert.assertEquals(1000L, recorder.getMaxStallMillis());
        Assert.assertTrue("the stack kept must belong to the longest stall. Actual: "
                + recorder.getMaxStallStackTrace(),
            recorder.getMaxStallStackTrace().contains("com.termux.app.Blocking.reflowBuffer"));
        Assert.assertFalse("a shorter later stall must not overwrite the longest stall's stack. Actual: "
                + recorder.getMaxStallStackTrace(),
            recorder.getMaxStallStackTrace().contains("com.termux.app.Blocking.scanOutput"));
    }

    @Test
    public void aStallWithNoSampleStillCountsAndSaysTheStackWasNotSampled() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.heartbeatPosted(1000L);
        recorder.heartbeatRan(1900L);

        Assert.assertEquals(1L, recorder.getStallCount());
        Assert.assertEquals(900L, recorder.getMaxStallMillis());
        Assert.assertEquals("not sampled", recorder.getMaxStallStackTrace());
    }

    @Test
    public void samplingWithoutAnOutstandingHeartbeatChangesNothing() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

        recorder.sampleWhileOutstanding(1400L, stackNaming("reflowBuffer"));

        Assert.assertEquals(0L, recorder.getStallCount());
        Assert.assertEquals("", recorder.getMaxStallStackTrace());
    }
}
