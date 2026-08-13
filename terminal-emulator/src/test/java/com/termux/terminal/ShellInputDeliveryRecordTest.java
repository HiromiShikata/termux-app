package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ShellInputDeliveryRecordTest {

    private static final String WRITER_FAILURE =
        "writing to the pseudo terminal failed: java.io.IOException: broken pipe";

    private static final long WRITE_MILLIS = 1783216800000L;

    @Test
    public void bytesTheShellNeverReceivedAreVisibleAsTheGapBetweenAcceptedAndWritten() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesAcceptedForDelivery(120, WRITE_MILLIS);
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

        record.recordBytesDiscardedBeforeDelivery(17, WRITE_MILLIS);

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

    @Test
    public void theTimeOfTheLastDiscardIsKeptSoALossCanBeTiedToTheGestureThatCausedIt() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesDiscardedBeforeDelivery(17, WRITE_MILLIS);
        record.recordBytesDiscardedBeforeDelivery(23, WRITE_MILLIS + 5000L);

        Assert.assertEquals("the discarded total is cumulative from process start, so without the time of"
                + " the most recent loss a report cannot say whether it happened during the gesture the"
                + " owner just made", Long.valueOf(WRITE_MILLIS + 5000L),
            record.getLastBytesDiscardedAtMillis());
    }

    @Test
    public void theTimeOfTheLastAcceptanceIsKeptSeparatelyFromTheTimeOfTheLastDiscard() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordBytesAcceptedForDelivery(120, WRITE_MILLIS);
        record.recordBytesDiscardedBeforeDelivery(17, WRITE_MILLIS + 5000L);

        Assert.assertEquals("a discard must not move the time of the last acceptance, otherwise input the"
                + " session refused would read as input it delivered", Long.valueOf(WRITE_MILLIS),
            record.getLastBytesAcceptedAtMillis());
        Assert.assertEquals(Long.valueOf(WRITE_MILLIS + 5000L), record.getLastBytesDiscardedAtMillis());
    }

    @Test
    public void aRecordWithNoDiscardAndNoAcceptanceHasNoTimeToReport() {
        ShellInputDeliveryRecord record = new ShellInputDeliveryRecord();

        record.recordWriterStarted();

        Assert.assertNull("returning any time for a discard that never happened would name a loss the"
            + " owner never suffered", record.getLastBytesDiscardedAtMillis());
        Assert.assertNull("returning any time for an acceptance that never happened would read as input"
            + " that was delivered", record.getLastBytesAcceptedAtMillis());
    }
}
