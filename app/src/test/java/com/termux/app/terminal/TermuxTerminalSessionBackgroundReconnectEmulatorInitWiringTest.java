package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalSessionBackgroundReconnectEmulatorInitWiringTest {

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

    @Test
    public void nonDisplayedReconnectInitializesReplacementSessionEmulatorOffScreen() throws IOException {
        String source = readClientSource();
        String methodBody = bodyBetween(source,
            "private TerminalSession reconnectDeadSessionPreservingDisplayedSession(",
            "private static final class ReconnectedSessionInputReplay");

        int displayedBranchIndex = methodBody.indexOf("if (displayedSession == deadSession)");
        Assert.assertTrue("displayed-session branch not found", displayedBranchIndex >= 0);
        String nonDisplayedBranch = bodyBetween(methodBody.substring(displayedBranchIndex),
            "} else {", "return newTerminalSession;");

        Assert.assertTrue(
            "the non-displayed (background) reconnect branch must initialize the replacement session's "
                + "emulator off-screen so the process spawns, not only notify the session list",
            nonDisplayedBranch.contains(
                "eagerInitializeReconnectedBackgroundSessionEmulator(newTerminalSession)"));
    }

    @Test
    public void offScreenReconnectInitializerComputesDimensionsUpdatesSizeAndForcesRepaint() throws IOException {
        String source = readClientSource();
        String initializerBody = bodyBetween(source,
            "private void eagerInitializeReconnectedBackgroundSessionEmulator(",
            "\n    private");

        Assert.assertTrue(
            "the off-screen reconnect initializer must run only when the emulator is not yet initialized",
            initializerBody.contains("getEmulator() != null"));
        Assert.assertTrue(
            "the off-screen reconnect initializer must compute the off-screen emulator dimensions",
            initializerBody.contains("computeSessionEmulatorDimensions()"));
        Assert.assertTrue(
            "the off-screen reconnect initializer must update the session size to spawn the process",
            initializerBody.contains(".updateSize("));
        Assert.assertTrue(
            "the off-screen reconnect initializer must force a remote repaint so output arrives",
            initializerBody.contains(".forceRemoteRepaint()"));
    }
}
