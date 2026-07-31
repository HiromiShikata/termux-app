package com.termux.app.diagnostics;

import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MainLooperQueueSnapshotInstrumentedTest {

    private static final int QUEUED_MESSAGE_COUNT = 7;

    private static final long QUEUED_MESSAGE_DELAY_MILLIS = 600000L;

    @Test
    public void snapshotCountsTheMessagesWaitingOnTheMainLooper() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable neverRun = () -> {
        };
        try {
            DiagnosticsMainLooperQueue before = MainLooperQueueSnapshot.take();
            for (int queued = 0; queued < QUEUED_MESSAGE_COUNT; queued++) {
                mainHandler.postDelayed(neverRun, QUEUED_MESSAGE_DELAY_MILLIS);
            }

            List<String> dumpLines = MainLooperQueueSnapshot.dumpMainLooper();
            DiagnosticsMainLooperQueue after = DiagnosticsMainLooperQueue.parse(dumpLines);

            assertTrue("the snapshot must count every message waiting on the main looper, counted "
                    + after.getPendingMessageCount() + " after queueing " + QUEUED_MESSAGE_COUNT
                    + " on top of " + before.getPendingMessageCount()
                    + ", from the dump\n" + String.join("\n", dumpLines),
                after.getPendingMessageCount() >= before.getPendingMessageCount() + QUEUED_MESSAGE_COUNT);
            assertTrue("the snapshot must name the target the waiting messages belong to, from the dump\n"
                    + String.join("\n", dumpLines),
                !after.getBusiestTargets().isEmpty()
                    && after.getBusiestTargets().get(0).getPendingMessageCount() >= QUEUED_MESSAGE_COUNT);
        } finally {
            mainHandler.removeCallbacks(neverRun);
        }
    }
}
