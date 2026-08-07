package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TerminalSessionRefusesUndeliverableInputTest {

    private static final String TERMINAL_SESSION_SOURCE_PATH =
        "src/main/java/com/termux/terminal/TerminalSession.java";

    @Test
    public void theWriteThatFeedsTheTerminalAsksWhetherTheInputCanReachTheProgramReadingIt()
            throws IOException {
        String writeBody = methodBody("public void write(byte[] data, int offset, int count) {");

        Assert.assertTrue(
            "user input must not be queued into a terminal whose reader cannot take it,"
                + " because those bytes stay in the line discipline and are replayed later",
            writeBody.contains("inputReachesTheProgramReadingTheTerminal()"));
    }

    @Test
    public void theSessionDecidesDeliverabilityFromTheRemoteClientCommandAndTheTerminalMode()
            throws IOException {
        String decisionBody = methodBody("public boolean inputReachesTheProgramReadingTheTerminal() {");

        Assert.assertTrue("the decision must come from TerminalInputDelivery",
            decisionBody.contains("TerminalInputDelivery.reachesTheProgramReadingTheTerminal("));
        Assert.assertTrue("the terminal mode must be read from the pty itself",
            decisionBody.contains("JNI.isPtyInCanonicalMode(mTerminalFileDescriptor)"));
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
