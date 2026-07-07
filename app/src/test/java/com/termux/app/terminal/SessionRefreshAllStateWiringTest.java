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
        Assert.assertTrue("the delayed rescan must be scheduled as the bounded adaptive retry rather than a "
                + "single fixed-delay post",
            methodBody.contains("PostReconnectStatuslineRescanRetry"));
        Assert.assertTrue("the first retry must fire at the delay decided by the backoff schedule planner",
            methodBody.contains("mPostReconnectStatuslineRescanRetryPlanner.firstAttemptDelayMillis()"));
    }

    @Test
    public void postReconnectRescanRetryIsBoundedAndStopsEarlyWhenStatuslinesAreParsed() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);

        int backoffIndex = source.indexOf("POST_RECONNECT_STATUSLINE_RESCAN_BACKOFF_MILLIS = {");
        Assert.assertTrue("the post-reconnect retry must use a named backoff schedule constant", backoffIndex >= 0);
        int backoffEnd = source.indexOf("};", backoffIndex);
        Assert.assertTrue(backoffEnd > backoffIndex);
        String backoffLiteral = source.substring(backoffIndex, backoffEnd);
        Assert.assertTrue("the first backoff entry must reuse the on-load rescan delay",
            backoffLiteral.contains("ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS"));

        int retryIndex = source.indexOf("private final class PostReconnectStatuslineRescanRetry");
        Assert.assertTrue("the bounded retry must be implemented as a dedicated runnable", retryIndex >= 0);
        int retryEnd = source.indexOf("\n    }", retryIndex);
        Assert.assertTrue(retryEnd > retryIndex);
        String retryBody = source.substring(retryIndex, retryEnd);

        Assert.assertTrue("each retry attempt must force a rescan that bypasses the skip-gate",
            retryBody.contains("repopulateStatuslineTimesForAllSessions(true)"));
        Assert.assertTrue("the retry must stop early once every reconnected session has a parsed statusline",
            retryBody.contains("allReconnectedSessionsHaveParsedStatusline("));
        Assert.assertTrue("the retry must be bounded by the backoff schedule planner so it cannot run unbounded",
            retryBody.contains("shouldScheduleNextAttempt("));
        Assert.assertTrue("each retry attempt must reschedule itself with a lightweight main-thread postDelayed",
            retryBody.contains("postDelayed(this"));
    }

    @Test
    public void reconnectInBackgroundReturnsTheReconnectedSessionNamesForTheRetryToTrack() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);
        int methodIndex =
            source.indexOf("private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String> reconnectableSessionNames) {");
        Assert.assertTrue("reconnect-in-background must return the reconnected session names", methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue("only sessions that were actually reconnected must be returned",
            methodBody.contains("reconnectedSessionNames.add(sessionName)"));
        Assert.assertTrue("the returned list must be the value the method produces",
            methodBody.contains("return reconnectedSessionNames"));
    }

    @Test
    public void displayedSessionCallScanTickReconnectsThenFullCallScansDisplayedSessionsNearLive() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);

        Assert.assertTrue("the fast displayed-session cycle must start when the activity resumes",
            source.contains("startDisplayedSessionCallScanTick();"));
        Assert.assertTrue("the fast displayed-session cycle must be torn down when the activity stops",
            source.contains("stopDisplayedSessionCallScanTick();"));

        int refreshIndex = source.indexOf("private void refreshDisplayedSessionsForCallToUser() {");
        Assert.assertTrue("the fast cycle must have a dedicated displayed-session refresh action", refreshIndex >= 0);
        int refreshEnd = source.indexOf("\n    }", refreshIndex);
        Assert.assertTrue(refreshEnd > refreshIndex);
        String refreshBody = source.substring(refreshIndex, refreshEnd);

        Assert.assertTrue("the fast cycle must select the displayed (non-hidden) session set, not the narrow visible set",
            refreshBody.contains("displayedSessionNames()"));
        Assert.assertTrue("the fast cycle must reconnect dead/hung displayed sessions so their accumulated output is pulled",
            refreshBody.contains("reconnectDeadDisplayedSessionsInBackground(displayedSessionNames)"));
        Assert.assertTrue("the fast cycle must run the full call-to-user transcript scan for every displayed session",
            refreshBody.contains("backgroundOutputTagsForSession(session)"));

        int selectorIndex = source.indexOf("private Set<String> displayedSessionNames() {");
        Assert.assertTrue("displayed-session selection must exist", selectorIndex >= 0);
        int selectorEnd = source.indexOf("\n    }", selectorIndex);
        Assert.assertTrue(selectorEnd > selectorIndex);
        String selectorBody = source.substring(selectorIndex, selectorEnd);
        Assert.assertTrue("the displayed set must be computed independently of whether the session-list sheet is open",
            !selectorBody.contains("sessionListOpen") && !selectorBody.contains("getOnScreenSessionNames"));
        Assert.assertTrue("hidden sessions must stay excluded when the owner is hiding them",
            selectorBody.contains("shouldHideHiddenSessions()") && selectorBody.contains("getDisabledSessionNames()"));
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
