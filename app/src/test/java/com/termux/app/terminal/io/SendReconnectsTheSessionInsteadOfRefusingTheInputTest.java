package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SendReconnectsTheSessionInsteadOfRefusingTheInputTest {

    private static final String CONTROLLER_SOURCE_PATH =
        "src/main/java/com/termux/app/terminal/io/TerminalEnterKeyController.java";

    @Test
    public void aSendThatCannotReachTheSessionReconnectsItBeforeTellingTheUserAnything()
            throws IOException {
        String submitBody = methodBody("public boolean submit(");

        int guardIndex = submitBody.indexOf("inputReachesTheProgramReadingTheTerminal()");
        int reconnectIndex = submitBody.indexOf("reconnectAndDeliverAfterReconnect(");
        int messageIndex = submitBody.indexOf("R.string.msg_terminal_input_not_delivered");

        Assert.assertTrue("submit must ask whether the session can receive the input", guardIndex >= 0);
        Assert.assertTrue("submit must reconnect the session that cannot receive the input",
            reconnectIndex > guardIndex);
        Assert.assertTrue("the message must only be reached when the reconnect could not be performed",
            messageIndex > reconnectIndex);
    }

    @Test
    public void theTypedTextIsHandedToTheReconnectSoItIsDeliveredWhenTheSessionComesBack()
            throws IOException {
        String reconnectBody = methodBody("private boolean reconnectAndDeliverAfterReconnect(");

        Assert.assertTrue("the reconnect must take the text that is still in the input field",
            reconnectBody.contains("editText.getText().toString()"));
        Assert.assertTrue("the reconnect must hand that text to the session client",
            reconnectBody.contains("reconnectFinishedSessionInPlace(session, pendingInput)"));
    }

    @Test
    public void theInputFieldIsClearedOnlyWhenTheReconnectTookTheText() throws IOException {
        String reconnectBody = methodBody("private boolean reconnectAndDeliverAfterReconnect(");

        int reconnectIndex = reconnectBody.indexOf("reconnectFinishedSessionInPlace(session, pendingInput)");
        int clearIndex = reconnectBody.indexOf("editText.setText(\"\")");

        Assert.assertTrue("the reconnect call must be made", reconnectIndex >= 0);
        Assert.assertTrue("the input field must be cleared only after the reconnect took the text",
            clearIndex > reconnectIndex);
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
