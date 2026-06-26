package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserPerTabWebViewWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

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

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void overlayHostsTabsThroughTheSharedWebViewHostAndPerTabFactory() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "new BrowserTabWebViewHost(mWebViewContainer, this::createWebViewForTab)"));
        Assert.assertTrue(source.contains(
            "private WebView createWebViewForTab(@NonNull BrowserTab tab)"));
        Assert.assertTrue(source.contains("WebView webView = new WebView(mActivity)"));
        Assert.assertTrue(source.contains("private final BrowserTabManager mTabManager"));
    }

    @Test
    public void overlayImplementsTheSharedTabSelectionListener() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "implements ProjectUrlOpener, BrowserTabSelectionListener"));
        Assert.assertTrue(source.contains("public void openTab(@NonNull BrowserTab tab)"));
        Assert.assertTrue(source.contains("public void closeTab(@NonNull BrowserTab tab)"));
        Assert.assertTrue(source.contains("public void promptNewTab()"));
        Assert.assertTrue(source.contains("public BrowserTab getActiveTab()"));
    }

    @Test
    public void overlayDrivesTheFaviconTabStripThroughTheSharedStripController() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "new BrowserTabFaviconStripController(tabStripScroll, tabStripContainer, this)"));
        String notifyBody = methodBody(source, "private void notifyTabsUpdated()");
        Assert.assertTrue(notifyBody.contains("mTabFaviconStripController.update("));
        Assert.assertTrue(notifyBody.contains("mTabManager.getTabs(PROJECT_BROWSER_SESSION_HANDLE)"));
    }

    @Test
    public void perTabClientsAttributeTitleFaviconAndContentToTheirOwnTabOnly() throws IOException {
        String factoryBody = methodBody(
            readControllerSource(), "private WebView createWebViewForTab(@NonNull BrowserTab tab)");
        Assert.assertTrue(factoryBody.contains("tab.setTitle(title)"));
        Assert.assertTrue(factoryBody.contains("tab.setFavicon(icon)"));
        Assert.assertTrue(factoryBody.contains("tab.setUrl(url)"));
        Assert.assertTrue(factoryBody.contains("isDisplayedTab(tab)"));
        Assert.assertTrue(readControllerSource().contains(
            "return mWebViewHost.getDisplayedTab() == tab;"));
    }

    @Test
    public void newWindowAndOpenInNewTabBothAddTabsRatherThanReplacingThePage() throws IOException {
        String factoryBody = methodBody(
            readControllerSource(), "private WebView createWebViewForTab(@NonNull BrowserTab tab)");
        Assert.assertTrue(factoryBody.contains("settings.setSupportMultipleWindows(true)"));
        Assert.assertTrue(factoryBody.contains(
            "public boolean openNewTabForUrl(@NonNull String url)"));
        Assert.assertTrue(factoryBody.contains(
            "openProjectUrlInNewTab(url, tab.getViewMode())"));
        Assert.assertTrue(factoryBody.contains(
            "openProjectUrlInNewTab(linkUrl, tab.getViewMode())"));
        String openInNewTabBody = methodBody(readControllerSource(),
            "public void openProjectUrlInNewTab(@NonNull String url, @NonNull BrowserViewMode viewMode)");
        Assert.assertTrue(openInNewTabBody.contains(
            "mTabManager.addTab(PROJECT_BROWSER_SESSION_HANDLE, normalizedUrl)"));
    }

    @Test
    public void closingATabDestroysItsOwnWebView() throws IOException {
        String closeTabBody = methodBody(readControllerSource(), "public void closeTab(@NonNull BrowserTab tab)");
        Assert.assertTrue(closeTabBody.contains("mWebViewHost.removeTab(tab)"));
    }

    @Test
    public void noSharedSingleWebViewFieldRemains() throws IOException {
        String source = readControllerSource();
        Assert.assertFalse(source.contains("private final WebView mWebView;"));
        Assert.assertFalse(source.contains("R.id.project_browser_web_view)"));
    }

    @Test
    public void layoutHostsTabsInAContainerAndShowsTheFaviconTabStripAboveTheFooter() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int containerIndex = layout.indexOf("@+id/project_browser_web_view_container");
        int stripScrollIndex = layout.indexOf("@+id/project_browser_tab_strip_scroll");
        int stripContainerIndex = layout.indexOf("@+id/project_browser_tab_strip_container");
        int footerIndex = layout.indexOf("@+id/project_browser_footer");
        Assert.assertTrue(containerIndex >= 0);
        Assert.assertTrue(stripScrollIndex >= 0);
        Assert.assertTrue(stripContainerIndex > stripScrollIndex);
        Assert.assertTrue("favicon tab strip must sit above the footer action row",
            stripScrollIndex < footerIndex);
        Assert.assertTrue("favicon tab strip must coexist with (not replace) the footer action row",
            footerIndex >= 0);
    }
}
