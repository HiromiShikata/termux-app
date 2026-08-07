package com.termux.app.diagnostics;

import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainThreadStallWatchdogInstrumentedTest {

    private static final long STALL_THRESHOLD_MILLIS = 250L;

    private static final long MAIN_THREAD_BLOCK_MILLIS = 3000L;

    private static final long STALL_RECORD_TIMEOUT_MILLIS = 5000L;

    private static final long STALL_RECORD_POLL_INTERVAL_MILLIS = 50L;

    @Test
    public void watchdogRecordsTheStackTraceOfTheCodeBlockingTheMainThread() {
        MainThreadStallRecorder recorder = new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);
        Thread watchdogThread = MainThreadStallWatchdog.startWatching(recorder);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                MainThreadStallWatchdogInstrumentedTest::blockTheMainThread);
            awaitRecordedStall(recorder);

            assertTrue("a blocked main thread must be recorded as a stall, recorded count was "
                    + recorder.getStallCount(),
                recorder.getStallCount() >= 1);
            assertTrue("the recorded stall must last at least the threshold, recorded "
                    + recorder.getMaxStallMillis() + " ms",
                recorder.getMaxStallMillis() >= STALL_THRESHOLD_MILLIS);
            assertTrue("the recorded stack trace must name the blocking method, recorded\n"
                    + recorder.getMaxStallStackTrace(),
                recorder.getMaxStallStackTrace().contains("blockTheMainThread"));
        } finally {
            watchdogThread.interrupt();
        }
    }

    private static void blockTheMainThread() {
        SystemClock.sleep(MAIN_THREAD_BLOCK_MILLIS);
    }

    private static void awaitRecordedStall(MainThreadStallRecorder recorder) {
        long deadlineMillis = SystemClock.uptimeMillis() + STALL_RECORD_TIMEOUT_MILLIS;
        while (recorder.getStallCount() == 0 && SystemClock.uptimeMillis() < deadlineMillis) {
            SystemClock.sleep(STALL_RECORD_POLL_INTERVAL_MILLIS);
        }
    }
}
