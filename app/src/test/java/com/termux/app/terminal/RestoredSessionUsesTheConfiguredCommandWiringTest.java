package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RestoredSessionUsesTheConfiguredCommandWiringTest {

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

    private String restorePersistedSessionsBody() throws IOException {
        String source = readClientSource();
        int start = source.indexOf("public boolean restorePersistedSessions()");
        Assert.assertTrue("the method that recreates the sessions of the previous run must exist for"
            + " this wiring to be assertable", start >= 0);
        int end = source.indexOf("public boolean restoreAlwaysPresentSessions()", start);
        Assert.assertTrue("the end of the method must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void aRestoredSessionIsGivenTheConfiguredCommandRatherThanTheStoredArgumentVector()
        throws IOException {
        String body = restorePersistedSessionsBody();

        Assert.assertTrue("the argument vector stored when a session was created is the command it"
                + " was started with at that moment. Replaying it means the session command the owner"
                + " configures never reaches a session that survives an app restart, so a session"
                + " keeps holding the number of operating-system processes its old command held,"
                + " however the owner changes the setting.",
            body.contains("RestoredSessionCommandPlanner")
                || body.contains("mRestoredSessionCommandPlanner"));
        Assert.assertTrue("the command to rebuild from is the configured one, read at the moment the"
                + " session is restored", body.contains("getAutosshCommand()"));
    }
}
