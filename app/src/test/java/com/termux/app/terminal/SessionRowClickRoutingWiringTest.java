package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionRowClickRoutingWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void tappingTheSessionRowAlwaysSwitchesEvenWhenReconnectHasFailed() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        int methodIndex = source.indexOf("private void onSessionRowClicked(int position) {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);

        Assert.assertFalse("tapping the session name/row must never short-circuit into a retry-only path "
                + "for a failed session; the row tap must always switch to the clicked session",
            methodBody.contains("retryReconnectAfterFailure"));
        Assert.assertTrue("tapping the session name/row must switch to the clicked session",
            methodBody.contains("switchToSessionReconnectingIfDead(clickedTerminalSession)"));
    }

    @Test
    public void reconnectFailedIndicatorClickListenerRetriesReconnectForItsOwnRowOnly() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        int bindMethodIndex = source.indexOf("private void bindSessionRowReconnectingIndicator(");
        Assert.assertTrue(bindMethodIndex >= 0);
        int bindMethodEnd = source.indexOf("\n    }", bindMethodIndex);
        Assert.assertTrue(bindMethodEnd > bindMethodIndex);
        String bindMethodBody = source.substring(bindMethodIndex, bindMethodEnd);

        Assert.assertTrue("the reconnect-failed indicator must be wired with its own click listener",
            bindMethodBody.contains("reconnectFailedIndicatorView.setOnClickListener("));
        Assert.assertTrue("the reconnect-failed indicator click listener must retry reconnect for this row's session",
            bindMethodBody.contains("retryReconnectFailedSession(sessionName)"));

        int retryMethodIndex = source.indexOf("private void retryReconnectFailedSession(");
        Assert.assertTrue(retryMethodIndex >= 0);
        int retryMethodEnd = source.indexOf("\n    }", retryMethodIndex);
        Assert.assertTrue(retryMethodEnd > retryMethodIndex);
        String retryMethodBody = source.substring(retryMethodIndex, retryMethodEnd);

        Assert.assertTrue("the reload button must retry the failed reconnect",
            retryMethodBody.contains("retryReconnectAfterFailure(sessionName)"));
        Assert.assertFalse("the reload button must not also switch to the session",
            retryMethodBody.contains("switchToSessionReconnectingIfDead"));
        Assert.assertFalse("the reload button must not also switch to the session",
            retryMethodBody.contains("setCurrentSession"));
    }
}
