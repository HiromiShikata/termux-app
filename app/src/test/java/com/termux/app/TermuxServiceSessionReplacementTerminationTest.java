package com.termux.app;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxServiceSessionReplacementTerminationTest {

    private static final String SERVICE_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxService.java";

    private static TerminalSession newTerminalSession() throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 100, null);
        markShellProcessAsRunning(terminalSession);
        return terminalSession;
    }

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

    private static void markShellProcessAsRunning(TerminalSession terminalSession) throws Exception {
        Field shellPidField = TerminalSession.class.getDeclaredField("mShellPid");
        shellPidField.setAccessible(true);
        shellPidField.setInt(terminalSession, RUNNING_SHELL_PROCESS_PID);
    }

    private static TermuxSession newTermuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, null, null, false);
    }

    private static void markShellProcessAsExited(TerminalSession terminalSession) throws Exception {
        Field shellPidField = TerminalSession.class.getDeclaredField("mShellPid");
        shellPidField.setAccessible(true);
        shellPidField.setInt(terminalSession, -1);
    }

    private static String readServiceSource() throws IOException {
        Path moduleRelative = Paths.get(SERVICE_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(SERVICE_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private static String bodyBetween(String source, String startMarker, String endMarker) {
        int startIndex = source.indexOf(startMarker);
        Assert.assertTrue(startMarker + " not found", startIndex >= 0);
        int endIndex = source.indexOf(endMarker, startIndex + startMarker.length());
        Assert.assertTrue(endMarker + " not found after " + startMarker, endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }

    @Test
    public void terminatingTheSessionBeingReplacedSendsSigkillToItsShellProcessGroup() throws IOException {
        String terminationBody = bodyBetween(readServiceSource(),
            "static void terminateSessionBeingReplaced(",
            "/** Remove a TermuxSession. */");

        Assert.assertTrue(
            "the session a reconnect replaces must have its shell process group signalled, otherwise the "
                + "shell and everything it started outlive the replacement",
            terminationBody.contains("sessionToRemove.finishIfRunning()"));
    }

    @Test
    public void replacementRemovalTerminatesTheOutgoingSessionBeforeItLeavesTheSessionList() throws IOException {
        String replacementRemovalBody = bodyBetween(readServiceSource(),
            "public synchronized int removeTermuxSessionBeingReplaced(",
            "static void terminateSessionBeingReplaced(");

        int terminationIndex = replacementRemovalBody.indexOf("terminateSessionBeingReplaced(sessionToRemove)");
        int removalIndex = replacementRemovalBody.indexOf("removeTermuxSession(sessionToRemove)");

        Assert.assertTrue("termination of the replaced session not found", terminationIndex >= 0);
        Assert.assertTrue("removal of the replaced session not found", removalIndex >= 0);
        Assert.assertTrue(
            "the replaced session must be terminated before it is removed from the session list",
            terminationIndex < removalIndex);
    }

    @Test
    public void terminatingARunningSessionBeingReplacedLeavesItRemovableFromTheSessionList() throws Exception {
        TerminalSession runningSession = newTerminalSession();
        Assert.assertTrue(runningSession.isRunning());

        List<TermuxSession> termuxSessions = new ArrayList<>();
        termuxSessions.add(newTermuxSession(runningSession));

        TermuxService.terminateSessionBeingReplaced(runningSession);
        boolean removed = TermuxService.removeFromTermuxSessions(termuxSessions, runningSession);

        Assert.assertTrue(removed);
        Assert.assertTrue(termuxSessions.isEmpty());
    }

    @Test
    public void replacingASessionWhoseProcessAlreadyExitedStillRemovesItWithoutFailing() throws Exception {
        TerminalSession exitedSession = newTerminalSession();
        markShellProcessAsExited(exitedSession);
        Assert.assertFalse(exitedSession.isRunning());

        List<TermuxSession> termuxSessions = new ArrayList<>();
        termuxSessions.add(newTermuxSession(exitedSession));

        TermuxService.terminateSessionBeingReplaced(exitedSession);
        boolean removed = TermuxService.removeFromTermuxSessions(termuxSessions, exitedSession);

        Assert.assertTrue(removed);
        Assert.assertTrue(termuxSessions.isEmpty());
    }

    @Test
    public void terminatingAMissingSessionIsIgnored() {
        TermuxService.terminateSessionBeingReplaced(null);
    }

    @Test
    public void plainSessionRemovalStillTerminatesNothing() throws IOException {
        String plainRemovalBody = bodyBetween(readServiceSource(),
            "public synchronized int removeTermuxSession(TerminalSession sessionToRemove) {",
            "static boolean removeFromTermuxSessions(");

        Assert.assertFalse(
            "plain session removal must not terminate any process group, so a session the user merely "
                + "switches away from keeps running",
            plainRemovalBody.contains("finishIfRunning()"));
    }

    @Test
    public void serviceShutdownStillKillsSessionsThroughTheExecutionCommandAwareKillPath() throws IOException {
        String shutdownBody = bodyBetween(readServiceSource(),
            "private synchronized void killAllTermuxExecutionCommands() {",
            "\n    private");

        Assert.assertTrue(
            "service shutdown must keep killing sessions through killIfExecuting so pending plugin results "
                + "are still processed",
            shutdownBody.contains("killIfExecuting(this, processResult)"));
        Assert.assertFalse(
            "service shutdown must not be routed through the reconnect replacement removal path",
            shutdownBody.contains("removeTermuxSessionBeingReplaced("));
        Assert.assertFalse(
            "service shutdown must not bypass killIfExecuting by terminating process groups directly",
            shutdownBody.contains("finishIfRunning()"));
    }
}
