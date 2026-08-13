package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UnchangedNotificationIsNotRepostedWiringTest {

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void theForegroundNotificationIsRepostedOnlyWhenWhatItShowsHasChanged() throws IOException {
        String source = readModuleSource("src/main/java/com/termux/app/TermuxService.java");

        int decisionIndex = source.indexOf("mForegroundNotificationRepostDecision.isNeededFor(");
        Assert.assertTrue("every genuine shell output reaches updateNotification through the activity"
                + " store change listener, so without this check the service posts the same notification"
                + " again on every output and pays two binder calls on the main thread for it",
            decisionIndex >= 0);
    }

    @Test
    public void theSessionsWaitingNotificationIsRepostedOnlyWhenTheCountHasChanged() throws IOException {
        String source = readModuleSource("src/main/java/com/termux/app/TermuxService.java");

        int decisionIndex = source.indexOf("mPendingCallNotificationRepostDecision.isNeededFor(");
        Assert.assertTrue("the count of sessions waiting on the owner is the whole content of this"
                + " notification, so reposting it while the count is unchanged redraws the same screen",
            decisionIndex >= 0);
    }
}
