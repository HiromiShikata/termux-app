package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnosticsDeliveryRecordsTheEnterTest {

    private static String deliverySource() throws IOException {
        return new String(Files.readAllBytes(
            Paths.get("src/main/java/com/termux/app/diagnostics/DiagnosticsReportSessionDelivery.java")),
            StandardCharsets.UTF_8);
    }

    @Test
    public void theDeliveryRecordsWhatHappenedToTheEnterItWrote() throws IOException {
        String source = deliverySource();

        Assert.assertTrue("a delivery that writes a carriage return and keeps no record of it leaves an"
                + " unsubmitted report unattributable, which is the whole gap this task exists to close: "
                + source,
            source.contains("DiagnosticsReportDeliveryRecorderHolder.getInstance().recordDelivery("));
    }

    @Test
    public void theAcceptedByteCountIsReadOnBothSidesOfTheEnterWrite() throws IOException {
        String source = deliverySource();

        int firstRead = source.indexOf("getBytesAcceptedForDelivery()");
        int enterWrite = source.indexOf("TerminalEnterKeyEncoder.enterSequence(");
        int secondRead = source.indexOf("getBytesAcceptedForDelivery()", enterWrite);
        Assert.assertTrue("the count has to be read before the write for a difference to exist: " + source,
            firstRead >= 0 && firstRead < enterWrite);
        Assert.assertTrue("without a read after the write there is nothing to compare against, and the"
            + " record would state an outcome it never observed: " + source, secondRead > enterWrite);
    }

    @Test
    public void whetherInputStillReachedTheProgramIsReadAfterThePaste() throws IOException {
        String source = deliverySource();

        int paste = source.indexOf("emulator.paste(");
        int checkAfterPaste = source.indexOf("inputReachesTheProgramReadingTheTerminal()", paste);
        Assert.assertTrue("the paste occupies the caller while the queue drains, so the state that held"
                + " before it says nothing about the state the carriage return was written in: " + source,
            paste >= 0 && checkAfterPaste > paste);
    }

    @Test
    public void thePasteIsTimedSoADeliveryLongEnoughToSpanAStateChangeIsVisible() throws IOException {
        String source = deliverySource();

        Assert.assertTrue("a delivery that occupied the caller for a long time is the one that could have"
                + " spanned a change in the session, and without the elapsed time that cannot be told from"
                + " a delivery that returned at once: " + source,
            source.contains("SystemClock.elapsedRealtime()"));
    }

    @Test
    public void theRecordedDeliveryReachesTheReportThatIsCollected() throws IOException {
        String collector = new String(Files.readAllBytes(
            Paths.get("src/main/java/com/termux/app/diagnostics/DiagnosticsReportCollector.java")),
            StandardCharsets.UTF_8);

        Assert.assertTrue("a delivery recorded where it happens but never read where the report is built"
                + " leaves the reading saying no report was ever delivered, which is worse than saying"
                + " nothing: " + collector,
            collector.contains("DiagnosticsReportDeliveryRecorderHolder.getInstance().snapshot()"));
    }
}
