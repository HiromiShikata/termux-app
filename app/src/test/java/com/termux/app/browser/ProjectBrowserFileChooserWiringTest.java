package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserFileChooserWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void projectControllerUsesSharedCoreWebChromeClient() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        Assert.assertTrue(source.contains("new BrowserCoreWebChromeClient("));
        Assert.assertTrue(source.contains("webView.setWebChromeClient(mWebChromeClient)"));
    }

    @Test
    public void projectControllerLaunchesFileChooserWithItsOwnRequestCode() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        Assert.assertTrue(source.contains("REQUEST_PROJECT_BROWSER_FILE_CHOOSER"));
        Assert.assertTrue(source.contains(
            "mActivity.startActivityForResult(intent, REQUEST_PROJECT_BROWSER_FILE_CHOOSER)"));
    }

    @Test
    public void projectControllerDeliversFileChooserResultToTheSharedClient() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void deliverFileChooserResult(int resultCode");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("displayedChromeClient.deliverFileChooserResult(resultCode, data)"));
    }

    @Test
    public void projectControllerCancelsPendingFileChooserOnDestroy() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);
        int methodIndex = source.indexOf("public void onActivityDestroy()");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("chromeClient.cancelPendingFileChooser()"));
    }

    @Test
    public void activityRoutesProjectBrowserFileChooserResultToProjectController() throws IOException {
        String source = readSource(ACTIVITY_RELATIVE_PATH);
        int methodIndex = source.indexOf("protected void onActivityResult(int requestCode");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains(
            "ProjectBrowserOverlayController.REQUEST_PROJECT_BROWSER_FILE_CHOOSER"));
        Assert.assertTrue(methodBody.contains(
            "mProjectBrowserOverlayController.deliverFileChooserResult(resultCode, data)"));
    }
}
