package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerPerTabWebViewWiringTest {

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
    public void eachTabGetsItsOwnWebViewBuiltByThePerTabFactory() throws IOException {
        String source = readControllerSource();

        Assert.assertTrue(source.contains(
            "new BrowserTabWebViewHost(mWebViewContainer, this::createWebViewForTab)"));
        Assert.assertTrue(source.contains(
            "private WebView createWebViewForTab(@NonNull BrowserTab tab)"));
        Assert.assertTrue(source.contains("WebView webView = new WebView(mActivity)"));
    }

    @Test
    public void perTabClientsAttributeTitleFaviconAndContentToTheirOwnTabOnly() throws IOException {
        String factoryBody = methodBody(
            readControllerSource(), "private WebView createWebViewForTab(@NonNull BrowserTab tab)");

        Assert.assertTrue(factoryBody.contains("tab.setTitle(title)"));
        Assert.assertTrue(factoryBody.contains("tab.setFavicon(icon)"));
        Assert.assertTrue(factoryBody.contains("tab.setUrl(url)"));
        Assert.assertFalse(factoryBody.contains("tabForUrlCallback"));
        Assert.assertFalse(factoryBody.contains("tabForTitleCallback"));
    }

    @Test
    public void displayedTabGuardsTheUiUpdatesSoBackgroundTabsNeverDriveTheChrome() throws IOException {
        String factoryBody = methodBody(
            readControllerSource(), "private WebView createWebViewForTab(@NonNull BrowserTab tab)");

        Assert.assertTrue(factoryBody.contains("isDisplayedTab(tab)"));
        Assert.assertTrue(readControllerSource().contains(
            "return mWebViewHost.getDisplayedTab() == tab;"));
    }

    @Test
    public void closingATabDestroysItsOwnWebView() throws IOException {
        String closeTabBody = methodBody(readControllerSource(), "public void closeTab(@NonNull BrowserTab tab)");

        Assert.assertTrue(closeTabBody.contains("mWebViewHost.removeTab(tab)"));
    }

    @Test
    public void removingASessionDestroysAllItsTabWebViews() throws IOException {
        String onRemovedBody = methodBody(
            readControllerSource(), "public void onSessionRemoved(@NonNull TerminalSession session)");

        Assert.assertTrue(onRemovedBody.contains("mWebViewHost.removeSession(session.mHandle)"));
    }

    @Test
    public void destroyingTheActivityTearsDownEveryPerTabWebView() throws IOException {
        String onDestroyBody = methodBody(readControllerSource(), "public void onActivityDestroy()");

        Assert.assertTrue(onDestroyBody.contains("mWebViewHost.destroyAll()"));
    }

    @Test
    public void noSharedSingleWebViewFieldRemains() throws IOException {
        String source = readControllerSource();

        Assert.assertFalse(source.contains("private final WebView mWebView;"));
        Assert.assertFalse(source.contains("R.id.browser_web_view)"));
    }
}
