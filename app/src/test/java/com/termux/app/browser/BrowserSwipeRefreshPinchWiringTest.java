package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserSwipeRefreshPinchWiringTest {

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private String readLayout() throws IOException {
        Path moduleRelative = Paths.get(LAYOUT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(LAYOUT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void sessionBrowserWebViewContainerIsWrappedInThePinchAwareSwipeRefreshLayout()
            throws IOException {
        String layout = readLayout();
        int swipeRefreshIndex = layout.indexOf("@+id/browser_swipe_refresh");
        int webViewContainerIndex = layout.indexOf("@+id/browser_web_view_container");
        int swipeRefreshCloseIndex = layout.indexOf(
            "</com.termux.app.browser.BrowserPinchAwareSwipeRefreshLayout>", swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshIndex >= 0);
        Assert.assertTrue(webViewContainerIndex > swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshCloseIndex > webViewContainerIndex);
    }

    @Test
    public void noBrowserWebViewRemainsWrappedInThePlainSwipeRefreshLayout() throws IOException {
        String layout = readLayout();
        Assert.assertFalse(layout.contains("androidx.swiperefreshlayout.widget.SwipeRefreshLayout"));
    }

    @Test
    public void projectBrowserWebViewIsWrappedInThePinchAwareSwipeRefreshLayout()
            throws IOException {
        String layout = readLayout();
        int swipeRefreshIndex = layout.indexOf("@+id/project_browser_swipe_refresh");
        int webViewIndex = layout.indexOf("@+id/project_browser_web_view");
        int swipeRefreshCloseIndex = layout.indexOf(
            "</com.termux.app.browser.BrowserPinchAwareSwipeRefreshLayout>", swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshIndex >= 0);
        Assert.assertTrue(webViewIndex > swipeRefreshIndex);
        Assert.assertTrue(swipeRefreshCloseIndex > webViewIndex);
    }
}
