package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TerminalSessionHoldsInputUntilTheShellStartsTest {

    private static final String TERMINAL_SESSION_SOURCE_PATH =
        "src/main/java/com/termux/terminal/TerminalSession.java";

    @Test
    public void inputSubmittedBeforeTheShellStartsIsHeldInsteadOfDiscarded() throws IOException {
        String writeBody = methodBody("public void write(byte[] data, int offset, int count) {");

        Assert.assertTrue(
            "input submitted while the session has no emulator is submitted against a solid black"
                + " screen, and discarding it loses what the user typed without any trace",
            writeBody.contains("holdsInputUntilTheShellStarts()"));
        Assert.assertTrue("the held bytes must be kept for the shell that is about to start",
            writeBody.contains("mShellStartupInputBuffer.hold(data, offset, count)"));
    }

    @Test
    public void theSessionOnlyHoldsInputWhileAShellIsStillExpectedToStart() throws IOException {
        String decisionBody = methodBody("private boolean holdsInputUntilTheShellStarts() {");

        Assert.assertTrue("a session that never built its emulator is the one showing solid black",
            decisionBody.contains("mEmulator == null"));
        Assert.assertTrue("a session whose runtime resources were released gets no further shell,"
                + " so holding its input would keep bytes that can never be delivered",
            decisionBody.contains("mRuntimeResourcesReleased"));
        Assert.assertTrue("a session with no shell path can never start a process, so holding its"
                + " input would keep it forever instead of reporting it as discarded",
            decisionBody.contains("mShellPath != null"));
        Assert.assertTrue("a session that already owned a shell is reconnected and replayed by the"
                + " layer above, so holding here as well would deliver the same input twice",
            decisionBody.contains("mShellProcessGeneration == 0"));
    }

    @Test
    public void theShellThatStartsReceivesTheInputThatWasHeldForIt() throws IOException {
        String initializeBody = methodBody(
            "public void initializeEmulator(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {");

        Assert.assertTrue("the input held while the screen was black must reach the shell once it exists",
            initializeBody.contains("deliverInputHeldUntilTheShellStarted()"));
    }

    private static String methodBody(String signature) throws IOException {
        String source = readTerminalSessionSource();
        int signatureIndex = source.indexOf(signature);
        Assert.assertTrue("TerminalSession.java must declare " + signature, signatureIndex >= 0);
        int bodyStart = signatureIndex + signature.length();
        int bodyEnd = source.indexOf("\n    }", bodyStart);
        Assert.assertTrue("the body of " + signature + " must be terminated", bodyEnd >= 0);
        return source.substring(bodyStart, bodyEnd);
    }

    private static String readTerminalSessionSource() throws IOException {
        File fromModuleDirectory = new File(TERMINAL_SESSION_SOURCE_PATH);
        File source = fromModuleDirectory.exists()
            ? fromModuleDirectory
            : new File("terminal-emulator/" + TERMINAL_SESSION_SOURCE_PATH);
        Assert.assertTrue("TerminalSession.java must be readable at " + source.getAbsolutePath(),
            source.exists());
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }
}
