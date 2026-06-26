package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverlayInputAndRefreshTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private static final String CORE_WEB_VIEW_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserCoreWebViewClient.java";

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String readControllerSource() throws IOException {
        return readModuleResource(CONTROLLER_RELATIVE_PATH);
    }

    private String readLayout() throws IOException {
        return readModuleResource(LAYOUT_RELATIVE_PATH);
    }

    @Test
    public void overlayWebViewContainerIsFocusableInTouchModeInLayout() throws IOException {
        String layout = readLayout();
        int webViewIndex = layout.indexOf("@+id/project_browser_web_view_container");
        Assert.assertTrue(webViewIndex >= 0);
        int closingBracket = layout.indexOf("/>", webViewIndex);
        Assert.assertTrue(closingBracket > webViewIndex);
        String webViewElement = layout.substring(webViewIndex, closingBracket);
        Assert.assertTrue(webViewElement.contains("android:focusable=\"true\""));
        Assert.assertTrue(webViewElement.contains("android:focusableInTouchMode=\"true\""));
    }

    @Test
    public void overlayRequestsWebViewFocusWhenShown() throws IOException {
        String source = readControllerSource();
        int showIndex = source.indexOf("private void show()");
        Assert.assertTrue(showIndex >= 0);
        int requestFocusIndex = source.indexOf("displayedWebView.requestFocus()", showIndex);
        int nextMethodIndex = source.indexOf("public void hide()", showIndex);
        Assert.assertTrue(requestFocusIndex > showIndex);
        Assert.assertTrue(requestFocusIndex < nextMethodIndex);
    }

    @Test
    public void overlayWebViewContainerIsWrappedInSwipeRefreshLayout() throws IOException {
        String layout = readLayout();
        int swipeRefreshIndex = layout.indexOf("@+id/project_browser_swipe_refresh");
        int webViewIndex = layout.indexOf("@+id/project_browser_web_view_container");
        int swipeRefreshCloseIndex =
            layout.indexOf("</com.termux.app.browser.BrowserPinchAwareSwipeRefreshLayout>", swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshIndex >= 0);
        Assert.assertTrue(webViewIndex > swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshCloseIndex > webViewIndex);
    }

    @Test
    public void overlayWiresSwipeRefreshToDisplayedWebViewReload() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "mSwipeRefreshLayout.setOnRefreshListener(this::reloadDisplayedWebView)"));
    }

    @Test
    public void overlayStopsRefreshingIndicatorOnPageFinished() throws IOException {
        String source = readControllerSource();
        int onPageFinishedIndex = source.indexOf("onPageFinished");
        Assert.assertTrue(onPageFinishedIndex >= 0);
        int setRefreshingFalseIndex =
            source.indexOf("mSwipeRefreshLayout.setRefreshing(false)", onPageFinishedIndex);
        Assert.assertTrue(setRefreshingFalseIndex > onPageFinishedIndex);
    }

    @Test
    public void overlayStopsRefreshingIndicatorOnMainFrameError() throws IOException {
        String source = readControllerSource();
        int onMainFrameErrorIndex = source.indexOf("private void handleMainFrameError()");
        Assert.assertTrue(onMainFrameErrorIndex >= 0);
        int setRefreshingFalseIndex =
            source.indexOf("mSwipeRefreshLayout.setRefreshing(false)", onMainFrameErrorIndex);
        Assert.assertTrue(setRefreshingFalseIndex > onMainFrameErrorIndex);
        String coreClientSource = readModuleResource(CORE_WEB_VIEW_CLIENT_RELATIVE_PATH);
        Assert.assertTrue(coreClientSource.contains("if (!request.isForMainFrame()) return;"));
    }
}
