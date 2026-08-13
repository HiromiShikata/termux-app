package com.termux.terminal;

import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ShellInputAfterTheWriterStoppedTest {

    private static final int SHELL_PROCESS_PID = 424242;

    private static final String WRITER_FAILURE =
        "writing to the pseudo terminal failed: java.io.IOException: broken pipe";

    private static final int LONGEST_WAIT_FOR_THE_WRITE_MILLIS = 2000;

    @Test
    public void inputWrittenAfterTheWriterStoppedDoesNotWaitForAReaderThatWillNeverCome()
            throws InterruptedException {
        TerminalSession session = sessionWhoseWriterStopped();
        fillTheQueueNothingDrains(session);

        Thread writingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                session.write(oneByte(), 0, 1);
            }
        });
        writingThread.setDaemon(true);
        writingThread.start();
        writingThread.join(LONGEST_WAIT_FOR_THE_WRITE_MILLIS);

        Assert.assertFalse("the terminal-to-process queue is drained only by the writer thread, so input"
                + " written once that thread has gone waits on a reader that will never arrive, and a"
                + " scroll gesture is written on the same thread that draws the application",
            writingThread.isAlive());
    }

    @Test
    public void inputWrittenAfterTheWriterStoppedIsCountedAsDiscardedRatherThanAcceptedForDelivery() {
        TerminalSession session = sessionWhoseWriterStopped();

        session.write(oneByte(), 0, 1);

        ShellInputDeliveryRecord deliveryRecord = session.getShellInputDeliveryRecord();
        Assert.assertEquals("input queued for a writer that has gone is lost, and counting it as"
                + " accepted reports a delivery that cannot happen",
            0L, deliveryRecord.getBytesAcceptedForDelivery());
        Assert.assertEquals("the loss has to be counted, otherwise the report shows nothing where the"
                + " owner's input disappeared", 1L, deliveryRecord.getBytesDiscardedBeforeDelivery());
    }

    @Test
    public void inputWrittenWhileTheWriterIsRunningIsStillAcceptedForDelivery() {
        TerminalSession session = sessionWhoseWriterIsRunning();

        session.write(oneByte(), 0, 1);

        Assert.assertEquals("a healthy session must keep delivering, so the writer state must gate only"
                + " the sessions whose writer has stopped",
            1L, session.getShellInputDeliveryRecord().getBytesAcceptedForDelivery());
    }

    private static TerminalSession sessionWhoseWriterIsRunning() {
        TerminalSession session =
            new TerminalSession(null, null, null, null, null, new TermuxTerminalSessionClientBase());
        session.mShellPid = SHELL_PROCESS_PID;
        session.getShellInputDeliveryRecord().recordWriterStarted();
        return session;
    }

    private static TerminalSession sessionWhoseWriterStopped() {
        TerminalSession session = sessionWhoseWriterIsRunning();
        session.getShellInputDeliveryRecord().recordWriterStopped(WRITER_FAILURE);
        return session;
    }

    private static void fillTheQueueNothingDrains(TerminalSession session) {
        byte[] everyByteTheQueueHolds =
            new byte[TerminalSession.TERMINAL_TO_PROCESS_IO_QUEUE_CAPACITY_BYTES];
        session.mTerminalToProcessIOQueue.write(everyByteTheQueueHolds, 0,
            everyByteTheQueueHolds.length);
    }

    private static byte[] oneByte() {
        return new byte[]{'x'};
    }
}
