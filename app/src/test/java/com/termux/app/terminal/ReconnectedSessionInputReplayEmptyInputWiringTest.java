package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReconnectedSessionInputReplayEmptyInputWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void replaySkipsTheSessionWriteEntirelyWhenThereIsNoPendingInput() throws IOException {
        String source = readClientSource();
        String body = methodBody(source, "private void replayPendingInputWhenConnected(");
        Assert.assertTrue("an empty or null pending input must not schedule any session write, because a "
                + "reconnect with nothing to replay would otherwise deliver a bare newline into the remote "
                + "pane and stack blank lines in TUI input boxes",
            body.contains("if (!ReconnectedSessionInputReplayPlanner.hasReplayableInput(pendingInput)) return;"));
        Assert.assertFalse("the null-coalescing concatenation must be gone: it converted a null or empty "
                + "pending input into a lone newline write",
            body.contains("pendingInput == null ? \"\" : pendingInput"));
    }

    @Test
    public void replayPayloadSubmitsWithCarriageReturnRatherThanInsertingANewline() throws IOException {
        String source = readClientSource();
        String body = methodBody(source, "private void replayPendingInputWhenConnected(");
        Assert.assertFalse("the replay payload must not terminate with \\n, which TUI applications treat "
                + "as insert-newline instead of submit",
            body.contains("+ \"\\n\""));
        Assert.assertTrue("the replay payload must come from the planner so the carriage-return submit "
                + "terminator is covered by unit tests",
            body.contains("ReconnectedSessionInputReplayPlanner.replayPayload(pendingInput)"));
    }
}
