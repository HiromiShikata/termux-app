package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserDownloadControllerWiringTest {

    private static final String DOWNLOAD_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/BrowserDownloadController.java";

    private static final String SESSION_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String PROJECT_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void downloadControllerEncapsulatesEnqueueRegisterAndQuery() throws IOException {
        String source = readModuleSource(DOWNLOAD_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("public void enqueueDownload"));
        Assert.assertTrue(source.contains("ACTION_DOWNLOAD_COMPLETE"));
        Assert.assertTrue(source.contains("RECEIVER_NOT_EXPORTED"));
        Assert.assertTrue(source.contains("public void unregisterDownloadCompleteReceiver"));
        Assert.assertTrue(source.contains("STATUS_SUCCESSFUL"));
        Assert.assertTrue(source.contains("ACTION_VIEW_DOWNLOADS"));
    }

    @Test
    public void sessionControllerEnqueuesThroughDownloadController() throws IOException {
        String source = readModuleSource(SESSION_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("new BrowserDownloadController("));
        Assert.assertTrue(source.contains(
            "mDownloadController.enqueueDownload(url, userAgent, contentDisposition, mimetype)"));
        Assert.assertTrue(source.contains("mDownloadController.unregisterDownloadCompleteReceiver()"));
    }

    @Test
    public void projectControllerGainsDownloadSupport() throws IOException {
        String source = readModuleSource(PROJECT_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("new BrowserDownloadController("));
        Assert.assertTrue(source.contains("webView.setDownloadListener("));
        Assert.assertTrue(source.contains(
            "mDownloadController.enqueueDownload(url, userAgent, contentDisposition, mimetype)"));
        Assert.assertTrue(source.contains("mDownloadController.unregisterDownloadCompleteReceiver()"));
    }
}
