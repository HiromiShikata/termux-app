package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReconnectedSessionInputReplayExhaustionWiringTest {

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

    private String replayExhaustionHandling() throws IOException {
        String source = readClientSource();
        int classIndex = source.indexOf(
            "private static final class ReconnectedSessionInputReplay implements Runnable {");
        Assert.assertTrue("the reconnect replay runnable must exist", classIndex >= 0);
        int classEnd = source.indexOf("\n    }", classIndex);
        Assert.assertTrue(classEnd > classIndex);
        String classBody = source.substring(classIndex, classEnd);
        int exhaustionBranch = classBody.indexOf("shouldScheduleAnotherAttempt");
        Assert.assertTrue("the exhaustion branch must exist", exhaustionBranch >= 0);
        return classBody.substring(exhaustionBranch);
    }

    @Test
    public void anExhaustedReplayWindowIsReportedInsteadOfDiscardingTheInputSilently() throws IOException {
        String exhaustionHandling = replayExhaustionHandling();
        Assert.assertTrue("the owner's typed input is consumed from the input box before the replay starts, "
                + "so a replay window that ends without the remote terminal becoming ready loses that input; "
                + "the loss must be logged rather than silenced",
            exhaustionHandling.contains("Logger.logError(LOG_TAG,"));
    }

    @Test
    public void theExhaustionReportDoesNotWriteTheOwnersTypedTextIntoTheOrdinaryLog() throws IOException {
        String exhaustionHandling = replayExhaustionHandling();
        Assert.assertFalse("the owner's message content must stay out of the ordinary log, so only its length "
                + "is reported",
            exhaustionHandling.contains("+ textToSend"));
        Assert.assertTrue("the report must carry the length of the lost input so the loss is diagnosable",
            exhaustionHandling.contains("textToSend.length()"));
    }
}
