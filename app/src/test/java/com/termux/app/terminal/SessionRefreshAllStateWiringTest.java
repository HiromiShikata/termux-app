package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionRefreshAllStateWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void reloadButtonPressRefreshesAllSessionStateOnDemand() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        int handlerIndex = source.indexOf("mLoadSessionButton.setOnClickListener");
        Assert.assertTrue(handlerIndex >= 0);
        int handlerEnd = source.indexOf("});", handlerIndex);
        Assert.assertTrue(handlerEnd > handlerIndex);
        String handlerBody = source.substring(handlerIndex, handlerEnd);
        Assert.assertTrue("the reload button must trigger the on-demand refresh-all action",
            handlerBody.contains("reloadSessionsAndRefreshAllState()"));
    }

    @Test
    public void reloadActionLoadsDefinitionsThenReconnectsAndRescans() throws IOException {
        String source = readSource(ACTIVITY_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void reloadSessionsAndRefreshAllState() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue("the reload action must keep loading the session definitions",
            methodBody.contains("loadSessionsFromDefinition()"));
        Assert.assertTrue("the reload action must also reconnect dead sessions and force a statusline rescan",
            methodBody.contains("reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline()"));
    }

    @Test
    public void fiveMinuteReconnectTickAlsoForcesAStatuslineRescan() throws IOException {
        String source = readSource(ACTIVITY_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void reconnectDeadDefinitionBackedSessions() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue("the periodic reconnect tick action must route through the reconnect-then-rescan path",
            methodBody.contains("reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline()"));
    }

    @Test
    public void combinedRefreshReconnectsDeadSessionsThenForceRescansEverySession() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);
        int methodIndex =
            source.indexOf("public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue("the combined refresh must reconnect dead definition-backed sessions",
            methodBody.contains("reconnectDeadDefinitionBackedSessionsInBackground()"));
        Assert.assertTrue("the combined refresh must force a statusline rescan that bypasses the skip-gate",
            methodBody.contains("repopulateStatuslineTimesForAllSessions(true)"));
        Assert.assertTrue("the rescan must be re-posted after a delay so reconnected sessions are picked up "
                + "once their emulators have rendered",
            methodBody.contains("postDelayed"));
        Assert.assertTrue("the delayed rescan must reuse the on-load rescan delay sequencing",
            methodBody.contains("ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS"));
    }

    @Test
    public void forcedStatuslineRescanBypassesTheSkipGateButStillRunsOffTheMainThread() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);
        int methodIndex =
            source.indexOf("private void repopulateStatuslineTimesForAllSessions(boolean forceRescan) {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue("a forced rescan must not consult the skip-gate to decide whether to scan",
            methodBody.contains("if (forceRescan) {"));
        Assert.assertTrue("a forced rescan must still record the scanned content version on the gate",
            methodBody.contains("mAllSessionsStatuslineScanGate.markScanned("));
        Assert.assertTrue("the heavy transcript read and parse must stay off the main thread",
            methodBody.contains("parseAndApplyStatuslineUpdatesOffThread("));
    }
}
