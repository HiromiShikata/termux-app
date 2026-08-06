package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TypedTerminalLineRecordsOwnerReplyTest {

    private static final String VIEW_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalViewClient.java";

    private String readViewClientSource() throws IOException {
        Path moduleRelative = Paths.get(VIEW_CLIENT_RELATIVE_PATH);
        Path path = Files.exists(moduleRelative)
            ? moduleRelative
            : Paths.get("app").resolve(VIEW_CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String methodBody(String signature) throws IOException {
        String source = readViewClientSource();
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void theTerminalViewClientRecordsTheGenuineOwnerReply() throws IOException {
        Assert.assertTrue("a line the owner types straight into the terminal view is a genuine reply, "
                + "so this client has to record the genuine in-app reply time that clears the red "
                + "owner-call dot instead of leaving it armed until the laggy statusline reply token "
                + "catches up",
            readViewClientSource().contains("SessionReplyTimeRecorder"));
    }

    @Test
    public void theCodePointThatEndsATypedLineRecordsTheOwnerReply() throws IOException {
        String body = methodBody(
            "public boolean onCodePoint(final int codePoint, boolean ctrlDown, TerminalSession session) {");
        Assert.assertTrue("every character the owner types into the terminal view arrives here, so "
                + "this is where a submitted typed line has to be turned into a recorded owner reply",
            body.contains("recordTypedTerminalLineReply"));
    }

    @Test
    public void theEnterKeyThatSubmitsATypedLineRecordsTheOwnerReply() throws IOException {
        String body = methodBody(
            "public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {");
        Assert.assertTrue("the enter key that submits a line typed into a running session is written "
                + "straight to the session by the terminal view, so this client has to record the "
                + "owner reply on that route as well",
            body.contains("recordTypedTerminalLineReplyOnEnter"));
    }

    @Test
    public void thePrintableCodePointRuleIsNotDuplicated() throws IOException {
        String source = readViewClientSource();
        int firstRule = source.indexOf("codePoint >= 0x20 && codePoint != 0x7F");
        Assert.assertTrue("the printable-code-point rule has to live in one place, not once in this "
                + "client and again in the typed-line tracking",
            firstRule < 0 || source.indexOf("codePoint >= 0x20 && codePoint != 0x7F", firstRule + 1) < 0);
    }
}
