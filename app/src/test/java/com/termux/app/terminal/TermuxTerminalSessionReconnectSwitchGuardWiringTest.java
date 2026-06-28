package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalSessionReconnectSwitchGuardWiringTest {

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
    public void onStartGuardsReconnectSwitchWithValidCurrentSessionCheck() throws IOException {
        String source = readClientSource();
        String onStartBody = bodyBetween(source, "public void onStart()", "public void onResume()");
        Assert.assertTrue(
            "onStart must only switch the displayed session on reconnect when none is displayed",
            onStartBody.contains(
                "shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession())"));
        int guardIndex = onStartBody.indexOf("shouldSwitchSessionOnReconnect(");
        int switchIndex = onStartBody.indexOf("setCurrentSession(getCurrentStoredSessionOrLast())");
        Assert.assertTrue(guardIndex >= 0);
        Assert.assertTrue("the reconnect switch must be guarded", switchIndex > guardIndex);
    }

    @Test
    public void restorePersistedSessionsGuardsSwitchWithValidCurrentSessionCheck() throws IOException {
        String source = readClientSource();
        String body = bodyBetween(source,
            "public boolean restorePersistedSessions()", "public boolean restoreAlwaysPresentSessions()");
        Assert.assertTrue(
            "restorePersistedSessions must not change the displayed session when one is already displayed",
            body.contains("shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession())"));
    }

    @Test
    public void restoreAlwaysPresentSessionsGuardsSwitchWithValidCurrentSessionCheck() throws IOException {
        String source = readClientSource();
        String body = bodyBetween(source,
            "public boolean restoreAlwaysPresentSessions()",
            "public void syncPersistedSessionsWithLiveSessions()");
        Assert.assertTrue(
            "restoreAlwaysPresentSessions must not change the displayed session when one is already displayed",
            body.contains("shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession())"));
    }

    @Test
    public void reconnectHelperGuardsStoredOrLastSwitchWithValidCurrentSessionCheck() throws IOException {
        String source = readClientSource();
        String body = bodyBetween(source,
            "public void setCurrentSessionOnReconnectIfNoneDisplayed()", "public void setCurrentStoredSession()");
        Assert.assertTrue(
            "the reconnect helper must guard the stored-or-last switch",
            body.contains("shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession())"));
        Assert.assertTrue(body.contains("setCurrentSession(getCurrentStoredSessionOrLast())"));
    }
}
