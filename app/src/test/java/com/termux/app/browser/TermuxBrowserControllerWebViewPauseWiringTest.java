package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerWebViewPauseWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void showingATabReappliesTheWebViewPauseState() throws IOException {
        String displayTabBody = methodBody(
            readControllerSource(),
            "private void displayTab(@NonNull BrowserTab tab, boolean forceReload)");
        Assert.assertTrue(displayTabBody.contains("applyWebViewPauseState()"));
    }

    @Test
    public void hidingTheBrowserReappliesTheWebViewPauseState() throws IOException {
        String hideBody = methodBody(readControllerSource(), "private void hideBrowserViews()");
        Assert.assertTrue(hideBody.contains("applyWebViewPauseState()"));
    }

    @Test
    public void activityStopPausesWebViewsAndFlushesHistory() throws IOException {
        String stopBody = methodBody(readControllerSource(), "public void onActivityStop()");
        Assert.assertTrue(stopBody.contains("mAppForegrounded = false"));
        Assert.assertTrue(stopBody.contains("applyWebViewPauseState()"));
        Assert.assertTrue(stopBody.contains("flushTabHistory()"));
    }

    @Test
    public void activityResumeResumesWebViews() throws IOException {
        String resumeBody = methodBody(readControllerSource(), "public void onActivityResume()");
        Assert.assertTrue(resumeBody.contains("mAppForegrounded = true"));
        Assert.assertTrue(resumeBody.contains("applyWebViewPauseState()"));
    }

    @Test
    public void applyPauseStateUsesTheVisibilityPlanForPauseResumeAndTimers() throws IOException {
        String applyBody = methodBody(readControllerSource(), "private void applyWebViewPauseState()");
        Assert.assertTrue(applyBody.contains("BrowserWebViewPausePlan.resolve("));
        Assert.assertTrue(applyBody.contains("webView.onResume()"));
        Assert.assertTrue(applyBody.contains("webView.onPause()"));
        Assert.assertTrue(applyBody.contains("resumeWebViewTimers()"));
        Assert.assertTrue(applyBody.contains("pauseWebViewTimers()"));
    }

    @Test
    public void persistTabHistoryGoesThroughTheDebouncingScheduler() throws IOException {
        String persistBody = methodBody(readControllerSource(), "private void persistTabHistory()");
        Assert.assertTrue(persistBody.contains("mTabHistoryPersistScheduler.markDirty(mTabHistory)"));
    }

    @Test
    public void recordHelpersSkipPersistWhenEntriesAreUnchanged() throws IOException {
        String source = readControllerSource();
        String recordBody = methodBody(source, "private void recordTabInHistory(@NonNull BrowserTab tab)");
        Assert.assertTrue(recordBody.contains("hasSameEntriesAs(mTabHistory)"));
        String bodySnippetBody = methodBody(
            source,
            "private void recordTabBodySnippetInHistory(@NonNull BrowserTab tab, @NonNull String bodySnippet)");
        Assert.assertTrue(bodySnippetBody.contains("hasSameEntriesAs(mTabHistory)"));
    }
}
