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
