package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DisconnectedSessionSubmitRecordsOwnerReplyTest {

    private static final String VIEW_PAGER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java";

    private String readViewPagerSource() throws IOException {
        Path moduleRelative = Paths.get(VIEW_PAGER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(VIEW_PAGER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String commitTextInputToTerminalBody() throws IOException {
        String source = readViewPagerSource();
        String signature = "public boolean commitTextInputToTerminal(";
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n        }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    private String reconnectBranch() throws IOException {
        String body = commitTextInputToTerminalBody();
        int reconnectStart = body.indexOf("reconnectFinishedSessionInPlace");
        Assert.assertTrue("the disconnected-session submit branch must reconnect in place",
            reconnectStart >= 0);
        int removeStart = body.indexOf("removeFinishedSession", reconnectStart);
        Assert.assertTrue(removeStart > reconnectStart);
        return body.substring(reconnectStart, removeStart);
    }

    @Test
    public void submittingOwnerContentIntoADisconnectedSessionRecordsTheOwnerReply() throws IOException {
        String reconnectBranch = reconnectBranch();
        Assert.assertTrue("owner content submitted into a disconnected session is delivered to the "
                + "reconnected session, so it is a genuine owner reply and must be recorded as one "
                + "instead of leaving the red owner-call dot armed until the laggy statusline reply "
                + "token catches up",
            reconnectBranch.contains("recordUserInputForSession"));
    }

    @Test
    public void anEmptySubmitIntoADisconnectedSessionIsNotRecordedAsAnOwnerReply() throws IOException {
        String reconnectBranch = reconnectBranch();
        Assert.assertTrue("an empty submit reconnects the session without sending any owner content, "
                + "so it must be gated out of the reply recording rather than clearing a genuinely "
                + "unanswered owner call",
            reconnectBranch.contains("ReconnectSubmitReplyDecision.shouldRecordReply"));
    }
}
