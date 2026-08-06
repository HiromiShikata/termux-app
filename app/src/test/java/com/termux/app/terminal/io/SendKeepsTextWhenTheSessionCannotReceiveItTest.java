package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SendKeepsTextWhenTheSessionCannotReceiveItTest {

    private static final String CONTROLLER_SOURCE_PATH =
        "src/main/java/com/termux/app/terminal/io/TerminalEnterKeyController.java";

    @Test
    public void theSendActionStopsBeforeConsumingTheTextWhenTheSessionCannotReceiveIt()
            throws IOException {
        String submitBody = methodBody("public boolean submit(");

        int guardIndex = submitBody.indexOf("inputReachesTheProgramReadingTheTerminal()");
        int commitIndex = submitBody.indexOf("commitToolbarTextInput(");
        int composingIndex = submitBody.indexOf("finishComposingText()");

        Assert.assertTrue("submit must ask whether the session can receive the input", guardIndex >= 0);
        Assert.assertTrue("submit must still commit the toolbar text input", commitIndex >= 0);
        Assert.assertTrue("submit must still finish the composing text", composingIndex >= 0);
        Assert.assertTrue(
            "the deliverability guard must run before the typed text is consumed, so a send that"
                + " cannot reach the session leaves the text in the input field",
            guardIndex < commitIndex && guardIndex < composingIndex);
    }

    @Test
    public void aSendThatCannotReachTheSessionTellsTheUserInsteadOfFailingSilently()
            throws IOException {
        String submitBody = methodBody("public boolean submit(");

        Assert.assertTrue("the user must be told that the input was not delivered",
            submitBody.contains("R.string.msg_terminal_input_not_delivered"));
    }

    @Test
    public void theEnterKeyIsNotWrittenIntoASessionThatCannotReceiveIt() throws IOException {
        String sendEnterBody = methodBody("private void sendEnter(");

        int guardIndex = sendEnterBody.indexOf("inputReachesTheProgramReadingTheTerminal()");
        int writeIndex = sendEnterBody.indexOf("session.write(");

        Assert.assertTrue("sendEnter must ask whether the session can receive the enter sequence",
            guardIndex >= 0);
        Assert.assertTrue("sendEnter must still write the enter sequence", writeIndex >= 0);
        Assert.assertTrue("the guard must run before the enter sequence is written",
            guardIndex < writeIndex);
    }

    private static String methodBody(String declarationPrefix) throws IOException {
        String source = readControllerSource();
        int declarationIndex = source.indexOf(declarationPrefix);
        Assert.assertTrue("TerminalEnterKeyController.java must declare " + declarationPrefix,
            declarationIndex >= 0);
        int bodyStart = source.indexOf(") {", declarationIndex);
        Assert.assertTrue("the parameter list of " + declarationPrefix + " must be terminated",
            bodyStart >= 0);
        int bodyEnd = source.indexOf("\n    }", bodyStart);
        Assert.assertTrue("the body of " + declarationPrefix + " must be terminated", bodyEnd >= 0);
        return source.substring(bodyStart, bodyEnd);
    }

    private static String readControllerSource() throws IOException {
        File fromModuleDirectory = new File(CONTROLLER_SOURCE_PATH);
        File source = fromModuleDirectory.exists()
            ? fromModuleDirectory
            : new File("app/" + CONTROLLER_SOURCE_PATH);
        Assert.assertTrue(
            "TerminalEnterKeyController.java must be readable at " + source.getAbsolutePath(),
            source.exists());
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }
}
