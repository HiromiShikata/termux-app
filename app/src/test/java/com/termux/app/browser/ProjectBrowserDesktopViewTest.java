package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserDesktopViewTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private static final String DESKTOP_VIEWPORT_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserDesktopViewportWebViewClient.java";

    private static final String INJECTION_CALL =
        "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)";

    private static final String INJECTION_INVOCATION = "injectDesktopViewport(view)";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String readControllerSource() throws IOException {
        return readModuleSource(CONTROLLER_RELATIVE_PATH);
    }

    private String readDesktopViewportClientSource() throws IOException {
        return readModuleSource(DESKTOP_VIEWPORT_CLIENT_RELATIVE_PATH);
    }

    @Test
    public void desktopUserAgentIsTheDesktopViewConfiguration() {
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Mobile"));
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Android"));
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("wv"));
    }

    @Test
    public void desktopViewportScriptForcesFullDesktopWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("width=1280"));
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("meta[name=\"viewport\"]"));
    }

    @Test
    public void projectBrowserAppliesDesktopUserAgent() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "settings.setUserAgentString(BrowserUserAgent.DESKTOP_USER_AGENT)"));
    }

    @Test
    public void projectBrowserUsesSharedDesktopViewportWebViewClient() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("new BrowserDesktopViewportWebViewClient()"));
    }

    @Test
    public void desktopViewportClientInjectsDesktopViewportScriptOnPageStart() throws IOException {
        String source = readDesktopViewportClientSource();
        Assert.assertTrue(source.contains(INJECTION_CALL));
        int onPageStartedIndex = source.indexOf("onPageStarted");
        int injectionIndex = source.indexOf(INJECTION_INVOCATION, onPageStartedIndex);
        Assert.assertTrue(onPageStartedIndex >= 0);
        Assert.assertTrue(injectionIndex > onPageStartedIndex);
    }

    @Test
    public void desktopViewportClientReinjectsDesktopViewportScriptOnPageFinish() throws IOException {
        String source = readDesktopViewportClientSource();
        int onPageFinishedIndex = source.indexOf("onPageFinished");
        Assert.assertTrue(onPageFinishedIndex >= 0);
        int injectionAfterPageFinishedIndex = source.indexOf(INJECTION_INVOCATION, onPageFinishedIndex);
        Assert.assertTrue(injectionAfterPageFinishedIndex > onPageFinishedIndex);
    }

    @Test
    public void desktopViewportClientReinjectsDesktopViewportScriptOnPageCommitVisible() throws IOException {
        String source = readDesktopViewportClientSource();
        int onPageCommitVisibleIndex = source.indexOf("onPageCommitVisible");
        Assert.assertTrue(onPageCommitVisibleIndex >= 0);
        int injectionAfterCommitVisibleIndex = source.indexOf(INJECTION_INVOCATION, onPageCommitVisibleIndex);
        Assert.assertTrue(injectionAfterCommitVisibleIndex > onPageCommitVisibleIndex);
    }

    @Test
    public void desktopViewportClientInjectsDesktopViewportScriptOnStartCommitAndFinish() throws IOException {
        String source = readDesktopViewportClientSource();
        int injectionCount = 0;
        int searchIndex = source.indexOf(INJECTION_INVOCATION);
        while (searchIndex >= 0) {
            injectionCount++;
            searchIndex = source.indexOf(INJECTION_INVOCATION, searchIndex + 1);
        }
        Assert.assertTrue(injectionCount >= 3);
    }

    @Test
    public void projectBrowserKeepsWideViewportAndOverviewMode() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("settings.setUseWideViewPort(true)"));
        Assert.assertTrue(source.contains("settings.setLoadWithOverviewMode(true)"));
    }
}
