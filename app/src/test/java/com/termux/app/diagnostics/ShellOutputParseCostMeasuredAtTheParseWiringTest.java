package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShellOutputParseCostMeasuredAtTheParseWiringTest {

    private String readSource(String modulePath) throws IOException {
        Path fromModule = Paths.get("..").resolve(modulePath);
        if (Files.exists(fromModule)) {
            return new String(Files.readAllBytes(fromModule), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get(modulePath)), StandardCharsets.UTF_8);
    }

    @Test
    public void theParseItselfIsTimedAndHandedToTheClient() throws IOException {
        String source = readSource("terminal-emulator/src/main/java/com/termux/terminal/TerminalSession.java");
        int start = source.indexOf("private void renderPendingShellOutput() {");
        Assert.assertTrue("the shell output of a session is parsed in one place and the measurement"
                + " belongs there", start >= 0);
        String body = source.substring(start, source.indexOf("\n        }", start));

        Assert.assertTrue("without timing the parse nothing in the report says how much of the main"
                + " thread every session's output consumes",
            body.contains("System.nanoTime()"));
        Assert.assertTrue("the measurement has to leave the session through the client, because the"
                + " emulator module does not depend on the application module",
            body.contains("mClient.onShellOutputParsed("));
    }

    @Test
    public void whatTheClientIsHandedReachesTheCounterTheReportReads() throws IOException {
        String source = readSource(
            "termux-shared/src/main/java/com/termux/shared/termux/terminal/TermuxTerminalSessionClientBase.java");
        int start = source.indexOf("public void onShellOutputParsed(");
        Assert.assertTrue("every session client the application uses extends this base, so recording"
                + " here is what keeps a backgrounded session's parse counted", start >= 0);
        String body = source.substring(start, source.indexOf("\n    }", start));

        Assert.assertTrue("a measurement that reaches no counter never reaches the report",
            body.contains("ShellOutputParseCostCounterHolder.getInstance().record("));
    }
}
