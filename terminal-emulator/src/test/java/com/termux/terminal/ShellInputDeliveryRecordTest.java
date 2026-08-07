package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ShellInputDeliveryRecordTest {

    private static final String WRITER_FAILURE =
        "writing to the pseudo terminal failed: java.io.IOException: broken pipe";

    @Test
    public void bytesTheShellNeverReceivedAreVisibleAsTheGapBetweenAcceptedAndWritten() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesAcceptedForDelivery(120);
        record.recordBytesWrittenToTheShell(40);

        Assert.assertEquals("what the application accepted from the owner must be counted, otherwise a"
                + " submission that never reached the shell cannot be told from one that did",
            120L, record.getBytesAcceptedForDelivery());
        Assert.assertEquals("what actually reached the shell must be counted separately from what was"
                + " accepted", 40L, record.getBytesWrittenToTheShell());
        Assert.assertEquals("the gap between the two names the loss directly",
            80L, record.getBytesAcceptedButNotWrittenYet());
    }

    @Test
    public void aWriterThatWroteMoreThanTheCountedAcceptanceNeverReportsANegativeGap() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesWrittenToTheShell(10);

        Assert.assertEquals("the two counters are advanced from different threads, so an interleaving"
                + " that reads the written total after the accepted one must not report a negative"
                + " undelivered figure in the report",
            0L, record.getBytesAcceptedButNotWrittenYet());
    }

    @Test
    public void bytesRefusedBeforeTheQueueAreCountedRatherThanDisappearing() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesDiscardedBeforeDelivery(17);

        Assert.assertEquals("a submission the session refused before it ever reached the queue is a"
                + " silent loss unless it is counted", 17L, record.getBytesDiscardedBeforeDelivery());
        Assert.assertEquals("a refused submission was never accepted, so it must not appear as"
                + " undelivered accepted bytes", 0L, record.getBytesAcceptedButNotWrittenYet());
    }

    @Test
    public void theWriterFailureIsKeptInsteadOfBeingSwallowed() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();
        record.recordWriterStarted();

        record.recordWriterStopped(WRITER_FAILURE);

        Assert.assertFalse("a session whose writer thread has exited still accepts submissions into a"
                + " queue nothing drains, so the writer state has to be reportable",
            record.isWriterRunning());
        Assert.assertEquals("the reason the writer stopped is the evidence that names the loss",
            WRITER_FAILURE, record.getWriterStoppedReason());
    }

    @Test
    public void aRestartedWriterReportsRunningAndNoLongerCarriesTheOldFailure() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();
        record.recordWriterStarted();
        record.recordWriterStopped(WRITER_FAILURE);

        record.recordWriterStarted();

        Assert.assertTrue("a session whose shell process was restarted has a live writer again",
            record.isWriterRunning());
        Assert.assertNull("keeping the previous generation's failure would report a live writer as"
                + " broken", record.getWriterStoppedReason());
    }
}
