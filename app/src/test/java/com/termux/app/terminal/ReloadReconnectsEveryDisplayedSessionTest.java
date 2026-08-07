package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReloadReconnectsEveryDisplayedSessionTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
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
    public void theReloadButtonPressReconnectsTheDisplayedSetRatherThanOnlyTheTerminalSession()
            throws IOException {
        String activitySource = readSource(ACTIVITY_RELATIVE_PATH);
        String reloadBody = methodBody(activitySource, "public void reloadSessionsAndRefreshAllState() {");

        Assert.assertTrue("the reload action must route through the displayed-set reconnect, because the "
                + "Load Sessions button hides the session list before reloading and the narrow visible set "
                + "then holds only the session shown in the terminal, leaving every other dead row "
                + "un-reconnected",
            reloadBody.contains("reconnectDeadDisplayedSessionsThenForceRescanStatusline()"));
    }

    @Test
    public void thePeriodicReconnectTickAlsoReconnectsTheDisplayedSet() throws IOException {
        String activitySource = readSource(ACTIVITY_RELATIVE_PATH);
        int schedulerIndex = activitySource.indexOf("new SessionReconnectScheduler(");
        Assert.assertTrue("the periodic reconnect scheduler must exist", schedulerIndex >= 0);
        int schedulerEnd = activitySource.indexOf(");", schedulerIndex);
        Assert.assertTrue(schedulerEnd > schedulerIndex);
        String schedulerConstruction = activitySource.substring(schedulerIndex, schedulerEnd);

        Assert.assertTrue("the periodic reconnect tick must run the displayed-set reconnect, otherwise a "
                + "session the owner never opens in the terminal stays disconnected indefinitely and no "
                + "automatic path ever reconnects it",
            schedulerConstruction.contains("reconnectDeadDisplayedSessions"));
    }

    @Test
    public void theDisplayedReconnectSelectsTheDisplayedSetAndNotTheNarrowVisibleSet() throws IOException {
        String clientSource = readSource(CLIENT_RELATIVE_PATH);
        String body = methodBody(clientSource,
            "public void reconnectDeadDisplayedSessionsThenForceRescanStatusline() {");

        Assert.assertTrue("the displayed-set refresh must reconnect every displayed (non-hidden) dead session",
            body.contains("reconnectDeadDisplayedSessionsInBackground(displayedSessionNames())"));
        Assert.assertTrue("the displayed-set refresh must not fall back to the narrow visible set",
            !body.contains("reconnectDeadDefinitionBackedSessionsInBackground()"));
        Assert.assertTrue("the displayed-set refresh must keep force-refreshing the statusline of every "
                + "displayed row so each reconnected row shows its own content",
            body.contains("repopulateStatuslineTimesForDisplayedSessions(true)"));
        Assert.assertTrue("the displayed-set refresh must keep tracking the reconnected session names for "
                + "the post-reconnect statusline rescan retry",
            body.contains("new PostReconnectStatuslineRescanRetry(reconnectedSessionNames)"));
    }

    @Test
    public void theUnhidePathKeepsTheNarrowSetSoItDoesNotReplaceTheSessionItJustRecreated()
            throws IOException {
        String listControllerSource =
            readSource("src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java");
        Assert.assertTrue("the unhide path must keep calling the narrow-set reconnect; widening it makes the "
                + "reconnect plan the session unhiding has just recreated, which replaces that session with "
                + "one holding no terminal emulator",
            listControllerSource.contains("mActivity.reconnectDeadDefinitionBackedSessions()"));

        String activitySource = readSource(ACTIVITY_RELATIVE_PATH);
        String narrowBody =
            methodBody(activitySource, "public void reconnectDeadDefinitionBackedSessions() {");
        Assert.assertTrue("the narrow-set entry point must stay on the narrow-set client action",
            narrowBody.contains("reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline()"));
    }

    @Test
    public void theDisplayedReconnectPathKeepsTheStaggeredSchedule() throws IOException {
        String clientSource = readSource(CLIENT_RELATIVE_PATH);
        String body = methodBody(clientSource,
            "private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String> reconnectableSessionNames) {");

        Assert.assertTrue("reconnecting a large displayed set must stay paced so the app is not frozen "
                + "by one reconnect per session at once",
            body.contains("mSessionReconnectPacer.enqueueSession(deadSession)"));
    }
}
