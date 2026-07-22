package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalSessionReconnectAmplificationGuardWiringTest {

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

    private String bodyBetween(String source, String startMarker, String endMarker) {
        int startIndex = source.indexOf(startMarker);
        Assert.assertTrue(startMarker + " not found", startIndex >= 0);
        int endIndex = source.indexOf(endMarker, startIndex + startMarker.length());
        Assert.assertTrue(endMarker + " not found after " + startMarker, endIndex > startIndex);
        return source.substring(startIndex, endIndex);
    }

    @Test
    public void reconnectDeadSessionGuardsAgainstDuplicateByCheckingForLiveSessionWithSameName() throws IOException {
        String source = readClientSource();
        String methodBody = bodyBetween(source,
            "private TerminalSession reconnectDeadSessionPreservingDisplayedSession(",
            "private static final class ReconnectedSessionInputReplay");

        int guardIndex = methodBody.indexOf("liveSessionWithNameAlreadyExists(");
        int createIndex = methodBody.indexOf("createTermuxSession(");

        Assert.assertTrue(
            "reconnectDeadSessionPreservingDisplayedSession must call liveSessionWithNameAlreadyExists before createTermuxSession",
            guardIndex >= 0);
        Assert.assertTrue(
            "liveSessionWithNameAlreadyExists guard must appear before createTermuxSession",
            guardIndex < createIndex);
    }
}
