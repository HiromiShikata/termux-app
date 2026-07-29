package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalSessionReconnectProcessTerminationWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String bodyBetween(String source, String startMarker, String endMarker) {
        int startIndex = source.indexOf(startMarker);
        Assert.assertTrue(startMarker + " not found", startIndex >= 0);
        int endIndex = source.indexOf(endMarker, startIndex + startMarker.length());
        Assert.assertTrue(endMarker + " not found after " + startMarker, endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }

    private String reconnectMethodBody(String source) {
        return bodyBetween(source,
            "private TerminalSession reconnectDeadSessionPreservingDisplayedSession(",
            "private static final class ReconnectedSessionInputReplay");
    }

    @Test
    public void reconnectRemovesTheReplacedSessionThroughThePathThatTerminatesItsProcessGroup() throws IOException {
        String methodBody = reconnectMethodBody(readClientSource());

        Assert.assertTrue(
            "reconnectDeadSessionPreservingDisplayedSession must remove the session it replaces through "
                + "removeTermuxSessionBeingReplaced so the outgoing shell and its children are terminated "
                + "instead of being orphaned",
            methodBody.contains("service.removeTermuxSessionBeingReplaced(deadSession)"));
    }

    @Test
    public void reconnectDoesNotRemoveTheReplacedSessionWithoutTerminatingIt() throws IOException {
        String methodBody = reconnectMethodBody(readClientSource());

        Assert.assertFalse(
            "reconnectDeadSessionPreservingDisplayedSession must not use the plain removeTermuxSession "
                + "call, which leaves a still-running shell alive after the session is dropped",
            methodBody.contains("service.removeTermuxSession(deadSession)"));
    }

    @Test
    public void reconnectTerminatesTheReplacedSessionBeforeCreatingTheReplacement() throws IOException {
        String methodBody = reconnectMethodBody(readClientSource());

        int terminatingRemovalIndex = methodBody.indexOf("removeTermuxSessionBeingReplaced(deadSession)");
        int replacementCreationIndex = methodBody.indexOf("createTermuxSession(");

        Assert.assertTrue("terminating removal of the replaced session not found", terminatingRemovalIndex >= 0);
        Assert.assertTrue("replacement session creation not found", replacementCreationIndex >= 0);
        Assert.assertTrue(
            "the replaced session's process group must be terminated before the replacement session is created",
            terminatingRemovalIndex < replacementCreationIndex);
    }

    @Test
    public void switchingToAnotherSessionWithoutReconnectingTerminatesNothing() throws IOException {
        String source = readClientSource();
        String switchMethodBody = bodyBetween(source,
            "public void switchToSessionReconnectingIfDead(",
            "private boolean shouldReconnectOnSwitch(");

        Assert.assertFalse(
            "switching to another session must not terminate any session process group",
            switchMethodBody.contains("finishIfRunning()"));
        Assert.assertFalse(
            "switching to another session must not use the replacement removal path",
            switchMethodBody.contains("removeTermuxSessionBeingReplaced("));
    }
}
