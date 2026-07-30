package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DisplayedSessionsAutoRefreshWiringTest {

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
    public void appLaunchOnStartForcesADisplayedSessionStatuslineRefresh() throws IOException {
        String source = readClientSource();
        String onStartBody = methodBody(source, "public void onStart() {");
        Assert.assertTrue("app launch must force-refresh the displayed (non-hidden) session set so every "
                + "displayed row shows current content without a manual Send",
            onStartBody.contains("repopulateStatuslineTimesForDisplayedSessions(true)"));
        Assert.assertTrue("the displayed-session launch refresh must be re-posted after the on-load delay so "
                + "a late-rendering statusline is still captured",
            onStartBody.contains("() -> repopulateStatuslineTimesForDisplayedSessions(true)")
                && onStartBody.contains("ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS"));
    }

    @Test
    public void reloadAndReconnectPathForcesADisplayedSessionStatuslineRefresh() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {");
        Assert.assertTrue("the reload / Load Sessions and periodic reconnect path must force-refresh the "
                + "displayed (non-hidden) session set so every displayed row updates without a manual Send",
            body.contains("repopulateStatuslineTimesForDisplayedSessions(true)"));
    }

    @Test
    public void displayedRefreshSelectsTheDisplayedSetNotTheNarrowVisibleSet() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "private void repopulateStatuslineTimesForDisplayedSessions(boolean forceRescan) {");
        Assert.assertTrue("the displayed refresh must select the displayed (non-hidden) session set",
            body.contains("displayedSessionNames()"));
        Assert.assertTrue("the displayed refresh must not fall back to the narrow visible set",
            !body.contains("visibleSessionNames()"));
    }

    @Test
    public void displayedRefreshIsStaggeredOffTheMainThreadForLargeSessionCounts() throws IOException {
        String source = readClientSource();
        String body = methodBody(source,
            "private void repopulateStatuslineTimesForDisplayedSessions(boolean forceRescan) {");
        Assert.assertTrue("the displayed refresh must chunk the transcript reads into bounded batches",
            body.contains("STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE"));
        Assert.assertTrue("later batches must still be posted to the main-thread handler so many displayed "
                + "sessions never read every transcript in one main-thread pass, and they must be posted "
                + "with no per-batch delay so no displayed session waits behind another session's batch for "
                + "its own statusline refresh",
            body.contains("mMainThreadHandler.postDelayed(")
                && body.contains("MAIN_THREAD_YIELD_WITHOUT_DELAY_MILLIS"));
    }

    @Test
    public void displayedRefreshReadsAndReparsesButSendsNoTerminalInput() throws IOException {
        String source = readClientSource();
        String displayedBody = methodBody(source,
            "private void repopulateStatuslineTimesForDisplayedSessions(boolean forceRescan) {");
        String coreBody = methodBody(source,
            "private void repopulateStatuslineTimesForSessionNames(@NonNull Set<String> sessionNames,");
        Assert.assertTrue("the displayed refresh must not write any input (newline/command) to a session",
            !displayedBody.contains(".write(") && !coreBody.contains(".write("));
        Assert.assertTrue("the displayed refresh must not encode or send an enter/newline sequence",
            !displayedBody.contains("enterSequence") && !coreBody.contains("enterSequence"));
        Assert.assertTrue("the refresh must go through the read-only statusline parse pipeline",
            coreBody.contains("statuslineScanText(")
                && coreBody.contains("parseAndApplyStatuslineUpdatesOffThread("));
    }
}
