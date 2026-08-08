package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxSessionRestoreBatchesSharedWorkTest {

    private static final String SERVICE_PATH = "src/main/java/com/termux/app/TermuxService.java";

    private static final String SESSION_CLIENT_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private String blockStartingAt(String source, String marker, String terminator) {
        int markerIndex = source.indexOf(marker);
        Assert.assertTrue("marker not found: " + marker, markerIndex >= 0);
        int blockEnd = source.indexOf(terminator, markerIndex);
        Assert.assertTrue("terminator not found after: " + marker, blockEnd > markerIndex);
        return source.substring(markerIndex, blockEnd);
    }

    @Test
    public void creatingASessionPublishesItsSharedStateThroughTheBatch() throws IOException {
        String creation = blockStartingAt(readModuleSource(SERVICE_PATH),
            "public synchronized TermuxSession createTermuxSession(ExecutionCommand executionCommand) {",
            "\n    }");

        Assert.assertTrue("the work every created session shares must go through the batch, so that"
                + " a restore of many sessions can publish it once",
            creation.contains("mSessionCreationBatch.runOrDefer(this::runWorkSharedByCreatedSessions)"));
        Assert.assertFalse("a notification update issued per created session is a synchronous binder"
                + " transaction per session on the main thread",
            creation.contains("updateNotification()"));
    }

    @Test
    public void restoringPersistedSessionsOpensAndClosesTheBatch() throws IOException {
        String restore = blockStartingAt(readModuleSource(SESSION_CLIENT_PATH),
            "List<PersistedSessionRestoreData> persistedSessions = loadPersistedSessions();",
            "notifySessionLimitExceeded(");

        Assert.assertTrue("restoring every persisted session at startup is one restore, not one"
                + " state change per session",
            restore.contains("beginSessionCreationBatch()"));
        Assert.assertTrue("the batch must close even when a session fails to restore",
            restore.contains("endSessionCreationBatch()"));
    }
}
