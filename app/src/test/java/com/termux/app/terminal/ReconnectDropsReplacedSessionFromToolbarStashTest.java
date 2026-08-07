package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReconnectDropsReplacedSessionFromToolbarStashTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        Path path = Files.exists(moduleRelative)
            ? moduleRelative
            : Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String methodBody(String signature) throws IOException {
        String source = readClientSource();
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    private void assertStashDropOutlivesTheSessionSwap(String body, String methodName) {
        int removal = body.lastIndexOf("TextInputForSession(");
        Assert.assertTrue(methodName + " must drop the replaced session from the toolbar text stash",
            removal >= 0);
        int swap = body.indexOf("setCurrentSession(newTerminalSession)");
        Assert.assertTrue(methodName + " must hand the terminal view to the replacement session",
            swap >= 0);
        Assert.assertTrue("setCurrentSession stashes the outgoing session under its own key, so a drop "
                + "that runs before the swap is undone by the swap itself and the replaced session stays "
                + "in a strong-keyed map for the lifetime of the activity, holding its emulator and the "
                + "whole scrollback with it: " + methodName,
            removal > swap);
    }

    @Test
    public void theInPlaceReconnectLeavesNothingKeyedByTheSessionItReplaced() throws IOException {
        assertStashDropOutlivesTheSessionSwap(
            methodBody("public boolean reconnectFinishedSessionInPlace("),
            "reconnectFinishedSessionInPlace");
    }

    @Test
    public void theBackgroundReconnectLeavesNothingKeyedByTheSessionItReplaced() throws IOException {
        assertStashDropOutlivesTheSessionSwap(
            methodBody("private TerminalSession reconnectDeadSessionPreservingDisplayedSession("),
            "reconnectDeadSessionPreservingDisplayedSession");
    }
}
