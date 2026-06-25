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
    public void projectBrowserAppliesMobileWebViewConfigurator() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "BrowserMobileWebViewConfigurator.apply(mWebView.getSettings())"));
    }

    @Test
    public void projectBrowserDoesNotApplyDesktopWebViewConfigurator() throws IOException {
        String source = readControllerSource();
        Assert.assertFalse(source.contains("BrowserDesktopWebViewConfigurator"));
    }

    @Test
    public void projectBrowserDoesNotOverrideUserAgentString() throws IOException {
        String source = readControllerSource();
        Assert.assertFalse(source.contains("settings.setUserAgentString("));
        Assert.assertFalse(source.contains("BrowserUserAgent.DESKTOP_USER_AGENT"));
    }

    @Test
    public void projectBrowserUsesMobileViewportWebViewClient() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("new BrowserMobileViewportWebViewClient()"));
    }

    @Test
    public void projectBrowserDoesNotUseDesktopViewportWebViewClient() throws IOException {
        String source = readControllerSource();
        Assert.assertFalse(source.contains("new BrowserDesktopViewportWebViewClient()"));
    }
}
