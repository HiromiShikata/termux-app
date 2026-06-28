package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserCoreWebViewClientWiringTest {

    private static final String CORE_CLIENT_PATH =
        "src/main/java/com/termux/app/browser/BrowserCoreWebViewClient.java";

    private static final String SESSION_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void coreClientExtendsHttpAuthClient() throws IOException {
        String source = readModuleSource(CORE_CLIENT_PATH);
        Assert.assertTrue(source.contains("class BrowserCoreWebViewClient extends BrowserHttpAuthWebViewClient"));
    }

    @Test
    public void coreClientInjectsDesktopViewportForDesktopMode() throws IOException {
        String source = readModuleSource(CORE_CLIENT_PATH);
        int injectIndex = source.indexOf("private void injectViewport");
        Assert.assertTrue(injectIndex >= 0);
        String body = source.substring(injectIndex);
        Assert.assertTrue(body.contains("getViewMode().isDesktop()"));
        Assert.assertTrue(body.contains("BrowserDesktopViewport.INJECTION_SCRIPT"));
        Assert.assertTrue(body.contains("shouldInjectMobileViewport()"));
        Assert.assertTrue(body.contains("BrowserMobileViewport.INJECTION_SCRIPT"));
    }

    @Test
    public void coreClientForwardsVisitedHistoryAndErrorLifecycle() throws IOException {
        String source = readModuleSource(CORE_CLIENT_PATH);
        Assert.assertTrue(source.contains("public void doUpdateVisitedHistory"));
        Assert.assertTrue(source.contains("mHost.onVisitedHistoryUpdated(view, url, isReload)"));
        Assert.assertTrue(source.contains("if (!request.isForMainFrame()) return;"));
        Assert.assertTrue(source.contains("mHost.onMainFrameError(view)"));
    }

    @Test
    public void coreClientSkipsViewportInjectionWhenPageDismissed() throws IOException {
        String source = readModuleSource(CORE_CLIENT_PATH);
        int finishedIndex = source.indexOf("public void onPageFinished");
        Assert.assertTrue(finishedIndex >= 0);
        String body = source.substring(finishedIndex);
        Assert.assertTrue(body.contains("boolean dismissed = mHost.onPageFinished(view, url)"));
        Assert.assertTrue(body.contains("if (dismissed) return;"));
    }

    @Test
    public void sessionControllerRoutesThroughSharedCoreClient() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host()"));
        Assert.assertFalse(source.contains("webView.setWebViewClient(new WebViewClient()"));
    }

    @Test
    public void sessionControllerDisablesMobileViewportInjection() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        int hostStart = source.indexOf("new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host()");
        Assert.assertTrue(hostStart >= 0);
        int shouldInjectIndex = source.indexOf("public boolean shouldInjectMobileViewport()", hostStart);
        Assert.assertTrue(shouldInjectIndex > hostStart);
        int returnFalseIndex = source.indexOf("return false;", shouldInjectIndex);
        int nextMethodIndex = source.indexOf("public void onPageStarted", shouldInjectIndex);
        Assert.assertTrue(returnFalseIndex > shouldInjectIndex && returnFalseIndex < nextMethodIndex);
    }

    @Test
    public void sessionControllerPreservesVisitedHistoryAndHttpAuthBehavior() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("public void onVisitedHistoryUpdated"));
        Assert.assertTrue(source.contains("if (isDisplayedTab(tab)) updatePageHeader();"));
    }
}
