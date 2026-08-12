package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class MainThreadStallSampleAttemptsTest {

    private static final long STALL_THRESHOLD_MILLIS = 80L;

    private static final StackTraceElement[] NO_FRAMES = new StackTraceElement[0];

    private static final StackTraceElement[] SOME_FRAMES = new StackTraceElement[]{
        new StackTraceElement("com.termux.app.TermuxActivity", "onCreate", "TermuxActivity.java", 343)
    };

    @Test
    public void aRunWithNoStallLeavesBothCountsAtZero() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(0L);
        recorder.heartbeatRan(10L);

        Assert.assertEquals(0L, recorder.getStackSampleAttemptCount());
        Assert.assertEquals(0L, recorder.getEmptyStackSampleCount());
    }

    @Test
    public void aSampleTheRuntimeAnsweredWithNoFramesIsCountedAsAnAttemptThatCameBackEmpty() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(0L);
        recorder.sampleWhileOutstanding(100L, NO_FRAMES);

        Assert.assertEquals("without the attempt count, a stall the watchdog never reached and a stall the"
                + " runtime refused to describe are the same line in the report",
            1L, recorder.getStackSampleAttemptCount());
        Assert.assertEquals(1L, recorder.getEmptyStackSampleCount());
    }

    @Test
    public void anEmptySampleLeavesTheStallOpenToBeingSampledAgain() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(0L);
        recorder.sampleWhileOutstanding(100L, NO_FRAMES);

        Assert.assertTrue("giving up after one empty answer throws away every later chance to name the"
            + " path that is blocking the main thread", recorder.needsStackSample(120L));

        recorder.sampleWhileOutstanding(120L, SOME_FRAMES);
        recorder.heartbeatRan(200L);

        Assert.assertEquals(2L, recorder.getStackSampleAttemptCount());
        Assert.assertEquals(1L, recorder.getEmptyStackSampleCount());
        Assert.assertTrue("the stack captured on the retry has to be the one the stall is recorded with."
                + " Actual: " + recorder.getMaxStallStackTrace(),
            recorder.getMaxStallStackTrace().contains("TermuxActivity.onCreate"));
    }

    @Test
    public void aSampleTakenBeforeTheThresholdIsNotCountedAsAnAttempt() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(0L);
        recorder.sampleWhileOutstanding(STALL_THRESHOLD_MILLIS - 1L, SOME_FRAMES);

        Assert.assertEquals("a wake-up the recorder rejects is not an attempt to describe a stall,"
            + " because at that point there is no stall yet", 0L, recorder.getStackSampleAttemptCount());
    }

    @Test
    public void theCountsReachTheReport() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        recorder.heartbeatPosted(0L);
        recorder.sampleWhileOutstanding(100L, NO_FRAMES);
        recorder.sampleWhileOutstanding(120L, SOME_FRAMES);
        recorder.heartbeatRan(200L);

        DiagnosticsMainThreadStalls stalls = DiagnosticsMainThreadStalls.of(recorder);

        Assert.assertEquals(2L, stalls.getStackSampleAttemptCount());
        Assert.assertEquals(1L, stalls.getEmptyStackSampleCount());
    }
}
