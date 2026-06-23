package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverlayBackNavigationWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void onBackPressedChecksBackForwardListBeforeGoingBackToAvoidAboutBlank() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("public boolean onBackPressed()");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("copyBackForwardList()"));
        Assert.assertTrue(methodBody.contains("getCurrentIndex()"));
        Assert.assertTrue(methodBody.contains("\"about:blank\""));
        Assert.assertTrue(methodBody.contains("hide()"));
    }

    @Test
    public void onPageFinishedClosesTheOverlayWhenAboutBlankIsLoadedWhileVisible() throws IOException {
        String source = readControllerSource();
        int onPageFinishedIndex = source.indexOf("public void onPageFinished(WebView view, String url)");
        Assert.assertTrue(onPageFinishedIndex >= 0);
        int methodEnd = source.indexOf("\n            }", onPageFinishedIndex);
        Assert.assertTrue(methodEnd > onPageFinishedIndex);
        String methodBody = source.substring(onPageFinishedIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mVisible"));
        Assert.assertTrue(methodBody.contains("\"about:blank\""));
        Assert.assertTrue(methodBody.contains("hide()"));
    }
}
