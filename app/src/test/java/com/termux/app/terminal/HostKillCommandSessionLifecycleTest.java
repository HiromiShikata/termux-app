package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;
import com.termux.app.terminal.session.FinishedSessionEnterAction;
import com.termux.app.terminal.session.TransientCommandSessionName;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HostKillCommandSessionLifecycleTest {

    private static final String CONNECT_TEMPLATE = "autossh -M 0 {name}";

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    @Test
    public void aSecondKillForTheSameHostSessionPlansTheSameCommandSessionName() {
        Assert.assertEquals("repeated taps must resolve to one command session, not to a growing pile of "
                + "identically-targeted sessions",
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName());
    }

    @Test
    public void aKillForADifferentHostSessionPlansItsOwnCommandSessionName() {
        Assert.assertNotEquals(
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "another-host-session").getSessionName());
    }

    @Test
    public void anExitedKillCommandSessionIsNeverPlannedForBackgroundReconnect() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession(
            TransientCommandSessionName.forKillOfSession("host-session"), false));

        List<String> sessionNamesToReconnect = new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, CONNECT_TEMPLATE);

        Assert.assertTrue("a short-lived command session must never be reconnected as if it were a host "
                + "session, otherwise a phantom connect session is created for a host session that does not exist",
            sessionNamesToReconnect.isEmpty());
    }

    @Test
    public void anOrdinaryHostSessionIsStillPlannedForBackgroundReconnect() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession("host-session", false));

        Assert.assertEquals(List.of("host-session"), new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, CONNECT_TEMPLATE));
    }

    @Test
    public void anExitedKillCommandSessionIsRemovedRatherThanReconnected() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            TransientCommandSessionName.forKillOfSession("host-session"), CONNECT_TEMPLATE);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
    }

    @Test
    public void theCommandSessionIsTakenOutOfTheSessionListAsSoonAsItsCommandHasExited() throws IOException {
        String source = readSource();
        int transientCheckIndex = source.indexOf(
            "if (TransientCommandSessionName.isTransient(finishedSession.mSessionName)) {");

        Assert.assertTrue("a finished command session must be removed from the list instead of lingering in it",
            transientCheckIndex >= 0);
        Assert.assertTrue("the removal must happen inside the finished-session handler",
            source.indexOf("removeFinishedSession(finishedSession);", transientCheckIndex) > transientCheckIndex);
    }

    @Test
    public void theCommandSessionIsStartedUnderItsPlannedTransientNameSoItIsNeverTreatedAsAHostSession()
            throws IOException {
        String methodBody = killHostSessionMethodBody();

        Assert.assertTrue("the command session must be created under the planned transient name, because an "
                + "ordinary name is planned for background reconnect and is shown as a host session row",
            methodBody.contains("workingDirectoryForNewSession(), false, commandSessionName)"));
    }

    @Test
    public void theCommandSessionIsNeverWrittenIntoThePersistedSessionStore() throws IOException {
        String methodBody = killHostSessionMethodBody();

        Assert.assertFalse("a short-lived command session must not survive a restart of the app",
            methodBody.contains("recordPersistedSession("));
    }

    @Test
    public void aRunningCommandSessionForTheSameHostSessionIsRevealedInsteadOfStartingAnotherOne()
            throws IOException {
        String methodBody = killHostSessionMethodBody();

        int revealIndex = methodBody.indexOf("revealExistingSessionByName(commandSessionName");
        int sessionCreationIndex = methodBody.indexOf("createTermuxSession(");

        Assert.assertTrue("a kill whose command session is still running must not start a second one",
            revealIndex >= 0);
        Assert.assertTrue("the existing command session must be looked up before another one is created",
            revealIndex < sessionCreationIndex);
    }

    @Test
    public void theConfiguredSessionCountCapIsHonouredBeforeAnotherCommandSessionIsStarted()
            throws IOException {
        String methodBody = killHostSessionMethodBody();

        int countIndex = methodBody.indexOf("cappedSessionCount(service)");
        int capComparisonIndex = methodBody.indexOf(">= maxSessions()");
        int sessionCreationIndex = methodBody.indexOf("createTermuxSession(");

        Assert.assertTrue("the live session count must be measured before a command session is started",
            countIndex >= 0);
        Assert.assertTrue("the measured count must be compared against the configured session cap",
            capComparisonIndex > countIndex);
        Assert.assertTrue("the cap must be checked before the session is created, not after",
            capComparisonIndex < sessionCreationIndex);
    }

    private String killHostSessionMethodBody() throws IOException {
        String source = readSource();
        int methodIndex = source.indexOf("public void killHostSession(final TerminalSession sessionToKill) {");
        Assert.assertTrue("killHostSession(TerminalSession) must still exist as a stable public entry point",
            methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    private String readSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(CLIENT_RELATIVE_PATH)),
            StandardCharsets.UTF_8);
    }
}
