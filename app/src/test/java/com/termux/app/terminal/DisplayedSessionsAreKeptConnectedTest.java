package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DisplayedSessionsAreKeptConnectedTest {

    private static final String SESSION_CLIENT_SOURCE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    @Test
    public void theBackgroundSweepAsksWhetherEachSessionCanStillReceiveInput() throws IOException {
        String body = methodBody(
            "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String>");

        Assert.assertTrue("the sweep must ask whether the session can receive input",
            body.contains("terminalSession.inputReachesTheProgramReadingTheTerminal()"));
        Assert.assertTrue("the sweep must require the state to have lasted before it reconnects",
            body.contains("mSessionInputDeliverabilityDwell.hasBeenUnableToReceiveInputLongEnough("));
    }

    @Test
    public void thatSignalReachesTheReconnectPlan() throws IOException {
        String body = methodBody(
            "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String>");

        int dwellIndex = body.indexOf("mSessionInputDeliverabilityDwell.hasBeenUnableToReceiveInputLongEnough(");
        int candidateIndex = body.indexOf("new DeadSessionReconnectPlanner.CandidateSession(");

        Assert.assertTrue("the dwell must be consulted", dwellIndex >= 0);
        Assert.assertTrue("the candidate must be built after the signal is known",
            candidateIndex > dwellIndex);
        Assert.assertTrue("the signal must be handed to the candidate",
            body.contains("unableToReceiveInputLongEnough));"));
    }

    @Test
    public void aSessionThatWasJustReconnectedStartsItsDwellAgain() throws IOException {
        String body = methodBody(
            "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String>");

        int enqueueIndex = body.indexOf("mSessionReconnectPacer.enqueueSession(deadSession)");
        int forgetIndex = body.indexOf("mSessionInputDeliverabilityDwell.forget(sessionName)");

        Assert.assertTrue("the reconnect must be enqueued", enqueueIndex >= 0);
        Assert.assertTrue(
            "the dwell must start again for a session that was just reconnected, so a session that is"
                + " slow to come back is not reconnected again immediately",
            forgetIndex > enqueueIndex);
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
