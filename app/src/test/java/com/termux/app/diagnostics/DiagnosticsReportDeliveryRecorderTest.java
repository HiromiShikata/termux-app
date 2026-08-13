package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class DiagnosticsReportDeliveryRecorderTest {

    @Test
    public void aProcessThatHasDeliveredNoReportSaysSoRatherThanReportingAnEmptyDelivery() {
        DiagnosticsReportDelivery delivery = new DiagnosticsReportDeliveryRecorder().snapshot();

        Assert.assertFalse("a delivery that never happened must not read as one that happened with"
            + " zero characters", delivery.wasAttempted());
    }

    @Test
    public void anEnterThatRaisedTheAcceptedByteCountIsRecordedAsAccepted() {
        DiagnosticsReportDelivery delivery =
            DiagnosticsReportDelivery.of("session-one", 11023, 842L, 4096L, 4097L, true);

        Assert.assertTrue("the byte count accepted for delivery rose across the write, which is the only"
            + " evidence the application has that the carriage return entered the queue",
            delivery.wasEnterAcceptedForDelivery());
    }

    @Test
    public void anEnterThatLeftTheAcceptedByteCountUnchangedIsRecordedAsNotAccepted() {
        DiagnosticsReportDelivery delivery =
            DiagnosticsReportDelivery.of("session-one", 11023, 842L, 4096L, 4096L, false);

        Assert.assertFalse("the write returned without the accepted count moving, so the carriage return"
            + " was discarded before delivery and the report was left unsubmitted",
            delivery.wasEnterAcceptedForDelivery());
        Assert.assertFalse("whether input still reached the program is what separates a closed queue from"
            + " a session that stopped taking input during the paste",
            delivery.didInputReachTheProgramAfterThePaste());
    }

    @Test
    public void theLastDeliveryReplacesTheOneBeforeIt() {
        DiagnosticsReportDeliveryRecorder recorder = new DiagnosticsReportDeliveryRecorder();
        recorder.recordDelivery(DiagnosticsReportDelivery.of("first", 10, 1L, 0L, 1L, true));
        recorder.recordDelivery(DiagnosticsReportDelivery.of("second", 20, 2L, 5L, 5L, false));

        DiagnosticsReportDelivery delivery = recorder.snapshot();

        Assert.assertEquals("a reading is taken to explain the delivery that just failed, so the newest"
            + " one is the one that has to survive", "second", delivery.getSessionName());
        Assert.assertEquals(20, delivery.getPastedCharacters());
        Assert.assertEquals(2L, delivery.getPasteMillis());
        Assert.assertFalse(delivery.wasEnterAcceptedForDelivery());
    }
}
