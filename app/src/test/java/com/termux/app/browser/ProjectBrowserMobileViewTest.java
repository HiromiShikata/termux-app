package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserMobileViewTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

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

    @Test
    public void projectBrowserAppliesSharedWebViewConfiguratorWithViewMode() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "BrowserWebViewConfigurator.apply(settings, tab.getViewMode(), mDefaultUserAgent)"));
    }

    @Test
    public void projectBrowserDoesNotUsePerModeConfigurators() throws IOException {
        String source = readControllerSource();
        Assert.assertFalse(source.contains("BrowserMobileWebViewConfigurator"));
        Assert.assertFalse(source.contains("BrowserDesktopWebViewConfigurator"));
    }

    @Test
    public void projectBrowserUsesSharedCoreWebViewClient() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host()"));
        Assert.assertFalse(source.contains("new BrowserMobileViewportWebViewClient()"));
    }

    @Test
    public void projectBrowserInjectsMobileViewportThroughSharedClient() throws IOException {
        String source = readControllerSource();
        int hostStart = source.indexOf("new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host()");
        Assert.assertTrue(hostStart >= 0);
        int shouldInjectIndex = source.indexOf("public boolean shouldInjectMobileViewport()", hostStart);
        Assert.assertTrue(shouldInjectIndex > hostStart);
        int returnTrueIndex = source.indexOf("return true;", shouldInjectIndex);
        int nextMethodIndex = source.indexOf("public void onPageStarted", shouldInjectIndex);
        Assert.assertTrue(returnTrueIndex > shouldInjectIndex && returnTrueIndex < nextMethodIndex);
    }
}
