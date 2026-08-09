package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HungSessionReconnectBackoffWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(CLIENT_RELATIVE_PATH)),
            StandardCharsets.UTF_8);
    }

    @Test
    public void silenceAloneDoesNotMakeASessionAReconnectCandidateOnEveryScan() throws IOException {
        String source = readClientSource();

        Assert.assertTrue("a session that is alive and merely quiet reports the same last-output time"
                + " after it is reconnected as it did before, so judging it on that time alone plans the"
                + " identical reconnect on every scan for as long as it stays quiet; the judgement has to"
                + " be paced by the record of what has already been attempted for that same silence",
            source.contains("isReadyToAttemptAgain("));
    }

    @Test
    public void aReconnectPlannedForSilenceIsRecordedSoTheNextScanCanSpaceItsRepeat() throws IOException {
        String source = readClientSource();

        Assert.assertTrue("nothing spaces the repeats unless each attempt made because a session was"
                + " silent is written down, so the enqueue loop has to record it the same way it clears"
                + " the input-deliverability dwell for the sessions it plans",
            source.contains("recordAttemptForSilence("));
    }

    @Test
    public void whichSessionsCountAsSilentIsDecidedBeforeAnyReconnectIsEnqueued() throws IOException {
        String source = readClientSource();
        int decisionIndex = source.indexOf("silentSessionLastOutTimeMillisByName.put(");
        int enqueueIndex = source.indexOf("mSessionReconnectPacer.enqueueSession(deadSession,");
        int recordIndex = source.indexOf("mHungSessionReconnectBackoff.recordAttemptForSilence(");

        Assert.assertTrue("the set of silent sessions is not decided anywhere", decisionIndex >= 0);
        Assert.assertTrue("the reconnect enqueue was not found", enqueueIndex >= 0);
        Assert.assertTrue("the attempt is never recorded", recordIndex >= 0);
        Assert.assertTrue("reading whether a session is still alive after its reconnect has been"
                + " enqueued would let a change in when the pacer acts silently stop the attempts being"
                + " recorded, which restores the loop this prevents",
            decisionIndex < enqueueIndex && enqueueIndex < recordIndex);
    }

    @Test
    public void aSessionWhoseShellProcessExitedIsNotPacedByTheSilenceBackoff() throws IOException {
        String source = readClientSource();
        int reconnectPlanningIndex = source.indexOf("planReconnects(candidateSessions");
        Assert.assertTrue("reconnect planning not found", reconnectPlanningIndex >= 0);

        Assert.assertTrue("a dead shell is a different failure from a quiet one and recovers by being"
                + " reconnected, so the pacing must be applied to the silence judgement only and never to"
                + " the running check that identifies a dead session",
            source.contains("mHungSessionDetector.isHung(lastOutTimeMillis, nowMillis)")
                && source.contains("boolean running = terminalSession.isRunning();"));
    }
}
