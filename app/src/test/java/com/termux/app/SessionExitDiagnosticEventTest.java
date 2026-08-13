package com.termux.app;

import com.termux.app.diagnostics.DiagnosticEventType;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SessionExitDiagnosticEventTest {

    private static String serviceSource() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/termux/app/TermuxService.java")),
            StandardCharsets.UTF_8);
    }

    private static String shellEndingRecorderSource() throws IOException {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/termux/app/terminal/ShellExitCountingTerminalSessionClient.java")),
            StandardCharsets.UTF_8);
    }

    private static String exitCallbackBody() throws IOException {
        String source = serviceSource();
        int start = source.indexOf("public void onTermuxSessionExited(");
        Assert.assertTrue("The exit callback must exist for this test to mean anything", start >= 0);
        int end = source.indexOf("private ShellCreateMode processShellCreateMode", start);
        Assert.assertTrue("The end of the exit callback must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void theEventTypeForASessionExitingOnItsOwnExists() {
        Assert.assertEquals("A session whose shell exits on its own needs its own event type, because it is a"
                + " different occurrence from a session the app removed deliberately",
            "SESSION_EXITED", DiagnosticEventType.valueOf("SESSION_EXITED").name());
    }

    @Test
    public void theShellEndingRecordsTheExitSoTheReportShowsTheFullSessionCycle() throws IOException {
        String source = shellEndingRecorderSource();

        Assert.assertTrue("Without this record a session whose shell exits vanishes from the diagnostics report,"
                + " so a create-exit-recreate loop is indistinguishable from a single healthy session: " + source,
            source.contains("DiagnosticEventType.SESSION_EXITED"));
    }

    @Test
    public void theRecordedExitNamesTheSessionThatExited() throws IOException {
        String source = shellEndingRecorderSource();

        Assert.assertTrue("An exit recorded without the session name cannot be matched against the create and remove"
                + " entries of the same session, which is the whole point of recording it: " + source,
            source.contains("finishedSession.mSessionName"));
    }

    @Test
    public void theExitIsRecordedWhereTheShellEndingIsObservedRatherThanWhereTheSessionLeavesTheList()
        throws IOException {
        String body = exitCallbackBody();

        Assert.assertTrue("The removal must still happen: " + body,
            body.contains("mShellManager.mTermuxSessions.remove(termuxSession)"));
        Assert.assertFalse("A session is taken off the list before its shell finishes ending on every reconnect, so"
                + " an exit recorded here is recorded only for the sessions that were removed after their process"
                + " had already ended, and every other ending is lost: " + body,
            body.contains("DiagnosticEventType.SESSION_EXITED"));
    }

    @Test
    public void aClientCannotObserveAShellEndingWithoutTheEndingBeingRecorded() throws IOException {
        String source = shellEndingRecorderSource();

        Assert.assertTrue("The recording is what makes the session population accountable, so a client that"
                + " overrides the callback must not be able to take the recording out with it, and a client that"
                + " returns early must not be able to skip it: " + source,
            source.contains("public final void onSessionFinished("));
    }
}
