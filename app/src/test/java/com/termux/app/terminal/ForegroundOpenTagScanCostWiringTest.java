package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ForegroundOpenTagScanCostWiringTest {

    private static String openTagsForSessionBody() throws IOException {
        String source = new String(Files.readAllBytes(
                Paths.get("src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java")),
            StandardCharsets.UTF_8);
        int start = source.indexOf("private void openTagsForSession(");
        Assert.assertTrue("The viewed-session open tag path must exist for this test to mean anything", start >= 0);
        int end = source.indexOf("private boolean shouldRunBackgroundOutputScan(", start);
        Assert.assertTrue("The end of the viewed-session open tag path must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void theViewedSessionTranscriptReadIsRecordedIntoItsOwnCostCounter() throws IOException {
        String body = openTagsForSessionBody();

        int transcriptReadIndex = body.indexOf("screen.getTranscriptText()");
        int recordIndex = body.indexOf("ForegroundOpenTagScanCostCounterHolder.getInstance()");
        Assert.assertTrue("The whole transcript of the viewed session is read here on every output change: "
            + body, transcriptReadIndex >= 0);
        Assert.assertTrue("Without this record the cost of the viewed session's own scan is absent from the"
                + " report, and the reported main-thread total understates the work being done: " + body,
            recordIndex > transcriptReadIndex);
    }

    @Test
    public void theRecordedCostCarriesTheTranscriptSizeItWasPaidFor() throws IOException {
        String body = openTagsForSessionBody();

        Assert.assertTrue("The transcript row count must be captured, because the point of the measurement is"
                + " to show whether the cost grows with the accumulated transcript: " + body,
            body.contains("screen.getActiveTranscriptRows()"));
        Assert.assertTrue("The elapsed time must be measured across the read and the scan: " + body,
            body.contains("System.nanoTime() - scanStartNanos"));
    }
}
