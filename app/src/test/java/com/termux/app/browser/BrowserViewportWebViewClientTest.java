package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserViewportWebViewClientTest {

    private static final String DESKTOP_VIEWPORT_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserDesktopViewportWebViewClient.java";

    private static final String MOBILE_VIEWPORT_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserMobileViewportWebViewClient.java";

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

    private String readDesktopViewportClientSource() throws IOException {
        return readModuleSource(DESKTOP_VIEWPORT_CLIENT_RELATIVE_PATH);
    }

    private String readMobileViewportClientSource() throws IOException {
        return readModuleSource(MOBILE_VIEWPORT_CLIENT_RELATIVE_PATH);
    }

    @Test
    public void desktopViewportScriptForcesFullDesktopWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("width=1280"));
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("meta[name=\"viewport\"]"));
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
    public void mobileViewportScriptPinsLayoutToDeviceWidthWithoutZoomOut() {
        Assert.assertTrue(BrowserMobileViewport.LAYOUT_CONTENT.contains("width=device-width"));
        Assert.assertTrue(BrowserMobileViewport.LAYOUT_CONTENT.contains("initial-scale=1"));
        Assert.assertTrue(BrowserMobileViewport.LAYOUT_CONTENT.contains("minimum-scale=1"));
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT.contains("minimum-scale=1"));
    }

    @Test
    public void mobileViewportClientDoesNotInjectDesktopViewportScript() throws IOException {
        String source = readMobileViewportClientSource();
        Assert.assertFalse(source.contains(INJECTION_CALL));
        Assert.assertFalse(source.contains(INJECTION_INVOCATION));
    }

    @Test
    public void mobileViewportClientInheritsHttpAuthHandling() throws IOException {
        String source = readMobileViewportClientSource();
        Assert.assertTrue(source.contains("extends BrowserHttpAuthWebViewClient"));
    }
}
