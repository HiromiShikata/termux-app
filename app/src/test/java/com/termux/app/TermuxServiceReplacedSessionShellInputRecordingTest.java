package com.termux.app;

import com.termux.app.diagnostics.DiagnosticsReplacedSessionShellInput;
import com.termux.app.diagnostics.ReplacedSessionShellInputRecorder;
import com.termux.terminal.ShellInputDeliveryRecord;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxServiceReplacedSessionShellInputRecordingTest {

    private static final String SERVICE_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxService.java";

    private static String readServiceSource() throws IOException {
        Path moduleRelative = Paths.get(SERVICE_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(SERVICE_RELATIVE_PATH)),
            StandardCharsets.UTF_8);
    }

    @Test
    public void whatTheReplacedSessionNeverWroteIsCarriedOverBeforeTheSessionIsDiscarded() {
        ShellInputDeliveryRecord deliveryRecord = new ShellInputDeliveryRecord();
        deliveryRecord.recordWriterStarted();
        deliveryRecord.recordBytesAcceptedForDelivery(4096);
        deliveryRecord.recordBytesWrittenToTheShell(1000);
        deliveryRecord.recordWriterStopped("broken pipe");
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        TermuxService.recordShellInputLostOnSessionBeingReplaced(recorder, "host-stuck", deliveryRecord);

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals("the replacement is the moment the evidence would otherwise be discarded, so it"
            + " has to be counted there", 1, snapshot.getSessionsReplacedWithInputUndelivered());
        Assert.assertEquals("what the user typed and the shell never received is the measurement of the"
            + " symptom", 3096L, snapshot.getWorstUndeliveredBytes());
        Assert.assertEquals("host-stuck", snapshot.getWorstUndeliveredSessionName());
        Assert.assertEquals("broken pipe", snapshot.getLastWriterStopReason());
    }

    @Test
    public void aReplacedSessionWithNoDeliveryRecordIsIgnoredRatherThanCountedAsAnEpisode() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        TermuxService.recordShellInputLostOnSessionBeingReplaced(recorder, "host-unknown", null);

        Assert.assertEquals(0, recorder.snapshot().getSessionsReplacedWithInputUndelivered());
    }

    @Test
    public void theCarryOverHappensBeforeTheReplacedSessionIsTerminated() throws IOException {
        String source = readServiceSource();
        int replacementRemovalIndex =
            source.indexOf("public synchronized int removeTermuxSessionBeingReplaced(");
        Assert.assertTrue("replacement removal not found", replacementRemovalIndex >= 0);
        String replacementRemovalBody = source.substring(replacementRemovalIndex,
            source.indexOf("static void recordShellInputLostOnSessionBeingReplaced("));

        int carryOverIndex =
            replacementRemovalBody.indexOf("recordShellInputLostOnSessionBeingReplaced(");
        int terminationIndex =
            replacementRemovalBody.indexOf("terminateSessionBeingReplaced(sessionToRemove)");

        Assert.assertTrue("the carry-over of what the session never wrote is not wired in", carryOverIndex >= 0);
        Assert.assertTrue("termination of the replaced session not found", terminationIndex >= 0);
        Assert.assertTrue("terminating first stops the writer and stamps that stop on every replacement,"
            + " which would report a writer stop on healthy sessions and bury the real failures",
            carryOverIndex < terminationIndex);
    }
}
