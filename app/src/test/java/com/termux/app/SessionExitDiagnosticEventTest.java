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
    public void theExitCallbackRecordsTheExitSoTheReportShowsTheFullSessionCycle() throws IOException {
        String body = exitCallbackBody();

        Assert.assertTrue("Without this record a session whose shell exits on its own vanishes from the diagnostics"
                + " report, so a create-exit-recreate loop is indistinguishable from a single healthy session: "
                + body,
            body.contains("DiagnosticEventType.SESSION_EXITED"));
    }

    @Test
    public void theExitIsRecordedOnlyWhenTheSessionWasStillInTheSessionList() throws IOException {
        String body = exitCallbackBody();

        int removeIndex = body.indexOf("mShellManager.mTermuxSessions.remove(termuxSession)");
        int recordIndex = body.indexOf("DiagnosticEventType.SESSION_EXITED");
        Assert.assertTrue("The removal must still happen: " + body, removeIndex >= 0);
        Assert.assertTrue("The record must be placed after the removal is attempted so its result can guard it: "
                + body,
            recordIndex > removeIndex);
        Assert.assertTrue("The removal result must guard the record, otherwise a callback for a session the app had"
                + " already removed would log a second exit for the same session and inflate the cycle count: "
                + body,
            body.contains("if (mShellManager.mTermuxSessions.remove(termuxSession))"));
    }
}
