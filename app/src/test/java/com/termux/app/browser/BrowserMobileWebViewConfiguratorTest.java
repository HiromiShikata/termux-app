package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserMobileWebViewConfiguratorTest {

    private static final String CONFIGURATOR_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/BrowserMobileWebViewConfigurator.java";

    private String readConfiguratorSource() throws IOException {
        Path moduleRelative = Paths.get(CONFIGURATOR_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONFIGURATOR_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void enablesWideViewPortSoDeviceWidthViewportIsHonored() throws IOException {
        String source = readConfiguratorSource();
        Assert.assertTrue(source.contains("settings.setUseWideViewPort(true)"));
        Assert.assertFalse(source.contains("settings.setUseWideViewPort(false)"));
    }

    @Test
    public void enablesOverviewModeSoPageFillsTheDeviceWidth() throws IOException {
        String source = readConfiguratorSource();
        Assert.assertTrue(source.contains("settings.setLoadWithOverviewMode(true)"));
        Assert.assertFalse(source.contains("settings.setLoadWithOverviewMode(false)"));
    }

    @Test
    public void doesNotOverrideUserAgentString() throws IOException {
        String source = readConfiguratorSource();
        Assert.assertFalse(source.contains("setUserAgentString"));
        Assert.assertFalse(source.contains("BrowserUserAgent.DESKTOP_USER_AGENT"));
    }
}
