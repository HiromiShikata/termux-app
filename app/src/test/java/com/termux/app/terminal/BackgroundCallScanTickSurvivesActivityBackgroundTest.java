package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackgroundCallScanTickSurvivesActivityBackgroundTest {

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
    public void theCallScanTickIsNotGatedOnTheActivityBeingVisible() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);

        Assert.assertTrue("starting the call-scan cycle must not depend on the activity being visible, "
                + "otherwise a call raised while the app is backgrounded is never detected",
            !methodBody(source, "public void startDisplayedSessionCallScanTick() {").contains("isVisible()"));
        Assert.assertTrue("the next call-scan tick must be scheduled even while the activity is not visible",
            !methodBody(source, "private void scheduleDisplayedSessionCallScanTick() {").contains("isVisible()"));
        Assert.assertTrue("the call-scan tick must scan even while the activity is not visible",
            !methodBody(source, "private void onDisplayedSessionCallScanTick() {").contains("isVisible()"));
    }

    @Test
    public void theCallScanTickIsTornDownOnDestroyRatherThanOnStop() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);

        Assert.assertTrue("the call-scan cycle must survive the activity stopping so a backgrounded app "
                + "keeps detecting calls",
            !methodBody(source, "public void onStop() {").contains("stopDisplayedSessionCallScanTick()"));
        Assert.assertTrue("the call-scan cycle must be torn down when the activity is destroyed",
            methodBody(source, "public void onDestroy() {").contains("stopDisplayedSessionCallScanTick()"));
    }

    @Test
    public void theActivityForwardsItsDestructionToTheSessionClient() throws IOException {
        String source = readSource(ACTIVITY_RELATIVE_PATH);

        Assert.assertTrue("the activity must forward onDestroy to the session client so the call-scan cycle "
                + "is released exactly once",
            methodBody(source, "public void onDestroy() {")
                .contains("mTermuxTerminalSessionActivityClient.onDestroy()"));
    }

    @Test
    public void theBackgroundCallScanKeepsTheStaggeredReconnectSchedule() throws IOException {
        String source = readSource(CLIENT_RELATIVE_PATH);
        String refreshBody = methodBody(source, "private void refreshDisplayedSessionsForCallToUser() {");

        Assert.assertTrue("the background cycle must keep reconnecting through the staggered displayed-session "
                + "path so a large session count never reconnects at once",
            refreshBody.contains("reconnectDeadDisplayedSessionsInBackground(displayedSessionNames)"));
        Assert.assertTrue("the background cycle must keep running the full call-to-user transcript scan",
            refreshBody.contains("backgroundOutputTagsForSession(session)"));
    }
}
