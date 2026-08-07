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

    private static final class MessageThatStaysQueued implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    public void snapshotCountsTheMessagesWaitingOnTheMainLooper() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        MessageThatStaysQueued messageThatStaysQueued = new MessageThatStaysQueued();
        try {
            for (int queued = 0; queued < QUEUED_MESSAGE_COUNT; queued++) {
                mainHandler.postDelayed(messageThatStaysQueued, QUEUED_MESSAGE_DELAY_MILLIS);
            }

            List<String> dumpLines = MainLooperQueueSnapshot.dumpMainLooper();
            DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

            assertTrue("the snapshot must count the messages waiting on the main looper, and the "
                    + QUEUED_MESSAGE_COUNT + " queued here cannot run for another "
                    + QUEUED_MESSAGE_DELAY_MILLIS + "ms, so at least that many must be counted."
                    + " It counted " + queue.getPendingMessageCount() + ", from the dump\n"
                    + String.join("\n", dumpLines),
                queue.getPendingMessageCount() >= QUEUED_MESSAGE_COUNT);
            assertTrue("the snapshot must attribute the waiting messages to the callback they belong"
                    + " to, and it attributed " + pendingMessagesAttributedToTheQueuedCallback(queue)
                    + " of the " + QUEUED_MESSAGE_COUNT + " queued here to "
                    + MessageThatStaysQueued.class.getName() + ", from the dump\n"
                    + String.join("\n", dumpLines),
                pendingMessagesAttributedToTheQueuedCallback(queue) >= QUEUED_MESSAGE_COUNT);
        } finally {
            mainHandler.removeCallbacks(messageThatStaysQueued);
        }
    }

    private static int pendingMessagesAttributedToTheQueuedCallback(
            DiagnosticsMainLooperQueue queue) {
        String queuedCallbackName = MessageThatStaysQueued.class.getName();
        for (DiagnosticsMainLooperQueueTarget target : queue.getBusiestTargets()) {
            if (target.getDescription().contains(queuedCallbackName)) {
                return target.getPendingMessageCount();
            }
        }
        return 0;
    }
}
