package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.TimeZone;

public class BackgroundedAppStatuslineCallRedDotTest {

    private static final String SESSION_NAME = "app";

    private static final String STATUSLINE_WHOSE_CALL_IS_NEWER_THAN_ITS_REPLY =
        "  call:2026-08-06T11:45:25Z out:2026-08-06T11:45:26Z reply:2026-08-06T11:04:42Z";

    private static final long NOW_MILLIS = Instant.parse("2026-08-06T12:05:00Z").toEpochMilli();

    private Path moduleResource(String relativePath) {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app").resolve(relativePath);
    }

    private String readModuleResource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(moduleResource(relativePath)), StandardCharsets.UTF_8);
    }

    @Test
    public void theBackgroundedSessionClientRecordsTheStatuslineTimesOfEveryOutput() throws IOException {
        String backgroundedSessionClient = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java");

        Assert.assertTrue(backgroundedSessionClient.contains("SessionStatuslineTimesRecorder"));
        Assert.assertFalse(backgroundedSessionClient.contains("does not parse\n        // the statusline"));
    }

    @Test
    public void bothSessionClientsBuildTheStatuslineScanTextTheSameWay() throws IOException {
        String backgroundedSessionClient = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxTerminalSessionServiceClient.java");
        String foregroundSessionClient = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java");

        Assert.assertTrue(backgroundedSessionClient.contains("SessionStatuslineScanText"));
        Assert.assertTrue(foregroundSessionClient.contains("SessionStatuslineScanText"));
    }

    @Test
    public void aStatuslineWhoseOnlyCallSignalIsItsCallTokenArmsTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        new SessionStatuslineReloadScanner().repopulateFromCurrentStatusline(store, SESSION_NAME,
            STATUSLINE_WHOSE_CALL_IS_NEWER_THAN_ITS_REPLY, NOW_MILLIS,
            TimeZone.getTimeZone("Asia/Tokyo"));

        Assert.assertFalse(STATUSLINE_WHOSE_CALL_IS_NEWER_THAN_ITS_REPLY.contains("<call-to-user>"));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION_NAME, NOW_MILLIS));
    }
}
