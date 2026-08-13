package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TerminalDrawCostMeasuredAtTheDrawWiringTest {

    private String readSource(String modulePath) throws IOException {
        Path fromModule = Paths.get("..").resolve(modulePath);
        if (Files.exists(fromModule)) {
            return new String(Files.readAllBytes(fromModule), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get(modulePath)), StandardCharsets.UTF_8);
    }

    @Test
    public void theDrawItselfIsTimedAndHandedToTheClient() throws IOException {
        String source = readSource("terminal-view/src/main/java/com/termux/view/TerminalView.java");
        int start = source.indexOf("protected void onDraw(Canvas canvas) {");
        Assert.assertTrue("the terminal has one draw entry point and the measurement belongs there",
            start >= 0);
        String body = source.substring(start, source.indexOf("\n    }", start));

        Assert.assertTrue("without timing the draw itself nothing in the report says how long the"
                + " terminal takes to put a frame on screen, which is what the owner feels when"
                + " scrolling stops working",
            body.contains("System.nanoTime()"));
        Assert.assertTrue("the measurement has to leave the view through the client, because the view"
                + " module does not depend on the application module",
            body.contains("mClient.onTerminalDrawn("));
    }

    @Test
    public void whatTheClientIsHandedReachesTheCounterTheReportReads() throws IOException {
        String source = readSource("app/src/main/java/com/termux/app/terminal/TermuxTerminalViewClient.java");
        int start = source.indexOf("public void onTerminalDrawn(");
        Assert.assertTrue("the application side has to receive the draw cost the view measures",
            start >= 0);
        String body = source.substring(start, source.indexOf("\n    }", start));

        Assert.assertTrue("a measurement that reaches no counter never reaches the report",
            body.contains("TerminalDrawCostCounterHolder.getInstance().record("));
    }
}
