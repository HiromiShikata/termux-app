package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserTabViewModeTest {

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
    public void tabDefaultsToDesktopViewMode() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        Assert.assertEquals(BrowserViewMode.DESKTOP, tab.getViewMode());
    }

    @Test
    public void viewModeCanBeSetDirectly() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setViewMode(BrowserViewMode.MOBILE);
        Assert.assertEquals(BrowserViewMode.MOBILE, tab.getViewMode());
        Assert.assertTrue(tab.getViewMode().isMobile());
    }

    @Test
    public void desktopFlagShimStaysConsistentWithViewMode() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setDesktopMode(false);
        Assert.assertEquals(BrowserViewMode.MOBILE, tab.getViewMode());
        Assert.assertFalse(tab.isDesktopMode());
        tab.setViewMode(BrowserViewMode.DESKTOP);
        Assert.assertTrue(tab.isDesktopMode());
    }

    @Test
    public void forDesktopFlagMapsBooleanToViewMode() {
        Assert.assertEquals(BrowserViewMode.DESKTOP, BrowserViewMode.forDesktopFlag(true));
        Assert.assertEquals(BrowserViewMode.MOBILE, BrowserViewMode.forDesktopFlag(false));
    }

    @Test
    public void sessionControllerDrivesConfiguratorFromTabViewMode() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains(
            "BrowserWebViewConfigurator.apply(webView, tab.getViewMode(), mEngineUserAgent)"));
        Assert.assertFalse(source.contains("tab.isDesktopMode() ? BrowserViewMode.DESKTOP : BrowserViewMode.MOBILE"));
    }

    @Test
    public void sessionControllerTogglesViewModeThroughEnum() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains(
            "activeTab.setViewMode(BrowserViewMode.forDesktopFlag(!activeTab.getViewMode().isDesktop()))"));
        Assert.assertTrue(source.contains(
            "tab.getViewMode().isDesktop()\n            ? BrowserUserAgent.desktopUserAgentFrom(mEngineUserAgent)\n"
                + "            : mEngineUserAgent"));
    }

    @Test
    public void persistedTabsStillSerializeDesktopFlagForCompatibility() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains(
            "new BrowserPersistedTab(tab.getUrl(), tab.getTitle(), tab.getViewMode().isDesktop())"));
    }
}
