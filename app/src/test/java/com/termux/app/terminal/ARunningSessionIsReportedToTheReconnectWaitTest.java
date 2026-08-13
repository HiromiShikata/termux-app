package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ARunningSessionIsReportedToTheReconnectWaitTest {

    private static final String SESSION_CLIENT_SOURCE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String BACKGROUND_SWEEP_DECLARATION =
        "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String>";

    @Test
    public void theSweepTellsTheWaitWhenItSeesASessionRunning() throws IOException {
        String body = methodBody(BACKGROUND_SWEEP_DECLARATION);

        Assert.assertTrue(
            "the sweep must report a running session to the wait, because being seen running is the only"
                + " evidence that separates a session which stayed up from one the wait itself held back",
            body.contains("mExitedSessionImmediateReconnectBackoff.recordObservedRunning(sessionName,"));
    }

    @Test
    public void theWaitIsToldOnlyAboutSessionsThatAreActuallyRunning() throws IOException {
        String body = methodBody(BACKGROUND_SWEEP_DECLARATION);

        int runningIndex = body.indexOf("boolean running = terminalSession.isRunning();");
        int reportIndex = body.indexOf("mExitedSessionImmediateReconnectBackoff.recordObservedRunning(");
        int guardIndex = body.indexOf("if (running) {");

        Assert.assertTrue("the sweep must establish whether the session is running", runningIndex >= 0);
        Assert.assertTrue("the report must come after that is known", reportIndex > runningIndex);
        Assert.assertTrue("the report must be guarded by the session actually running, otherwise a dead"
                + " session would reset the wait that is holding it back",
            guardIndex > runningIndex && guardIndex < reportIndex);
    }

    @Test
    public void theWaitIsToldBeforeItIsAskedWhetherTheSessionMayReconnect() throws IOException {
        String body = methodBody(BACKGROUND_SWEEP_DECLARATION);

        int reportIndex = body.indexOf("mExitedSessionImmediateReconnectBackoff.recordObservedRunning(");
        int askIndex = body.indexOf("mExitedSessionImmediateReconnectBackoff.isReadyToReconnectImmediately(");

        Assert.assertTrue("the report must be made", reportIndex >= 0);
        Assert.assertTrue("the wait must be asked", askIndex >= 0);
        Assert.assertTrue("the observation must reach the wait before the same scan asks it whether the"
                + " session may reconnect, so the answer uses this scan's evidence", askIndex > reportIndex);
    }

    private static String methodBody(String declarationPrefix) throws IOException {
        String source = readSessionClientSource();
        int declarationIndex = source.indexOf(declarationPrefix);
        Assert.assertTrue(
            "TermuxTerminalSessionActivityClient.java must declare " + declarationPrefix,
            declarationIndex >= 0);
        int bodyStart = source.indexOf(") {", declarationIndex);
        Assert.assertTrue("the parameter list of " + declarationPrefix + " must be terminated",
            bodyStart >= 0);
        int bodyEnd = source.indexOf("\n    }", bodyStart);
        Assert.assertTrue("the body of " + declarationPrefix + " must be terminated", bodyEnd >= 0);
        return source.substring(bodyStart, bodyEnd);
    }

    private static String readSessionClientSource() throws IOException {
        File fromModuleDirectory = new File(SESSION_CLIENT_SOURCE_PATH);
        File source = fromModuleDirectory.exists()
            ? fromModuleDirectory
            : new File("app/" + SESSION_CLIENT_SOURCE_PATH);
        Assert.assertTrue(
            "TermuxTerminalSessionActivityClient.java must be readable at " + source.getAbsolutePath(),
            source.exists());
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }
}
