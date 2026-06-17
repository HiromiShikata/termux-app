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

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
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
    public void projectBrowserInjectsDesktopViewportScriptOnPageStart() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)"));
        int onPageStartedIndex = source.indexOf("onPageStarted");
        int injectionIndex = source.indexOf(
            "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)");
        Assert.assertTrue(onPageStartedIndex >= 0);
        Assert.assertTrue(injectionIndex > onPageStartedIndex);
    }

    @Test
    public void projectBrowserReinjectsDesktopViewportScriptOnPageFinish() throws IOException {
        String source = readControllerSource();
        int onPageFinishedIndex = source.indexOf("onPageFinished");
        Assert.assertTrue(onPageFinishedIndex >= 0);
        int injectionAfterPageFinishedIndex = source.indexOf(
            "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)", onPageFinishedIndex);
        Assert.assertTrue(injectionAfterPageFinishedIndex > onPageFinishedIndex);
    }

    @Test
    public void projectBrowserReinjectsDesktopViewportScriptOnPageCommitVisible() throws IOException {
        String source = readControllerSource();
        int onPageCommitVisibleIndex = source.indexOf("onPageCommitVisible");
        Assert.assertTrue(onPageCommitVisibleIndex >= 0);
        int injectionAfterCommitVisibleIndex = source.indexOf(
            "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)", onPageCommitVisibleIndex);
        Assert.assertTrue(injectionAfterCommitVisibleIndex > onPageCommitVisibleIndex);
    }

    @Test
    public void projectBrowserInjectsDesktopViewportScriptOnStartCommitAndFinish() throws IOException {
        String source = readControllerSource();
        int injectionCount = 0;
        int searchIndex = source.indexOf("view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)");
        while (searchIndex >= 0) {
            injectionCount++;
            searchIndex = source.indexOf(
                "view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null)", searchIndex + 1);
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
