package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionListBottomSheetBrowserHideWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void showInvokesTheBrowserHideStep() throws IOException {
        String source = readControllerSource();
        int showIndex = source.indexOf("public void show() {");
        Assert.assertTrue(showIndex >= 0);
        int nextMethodIndex = source.indexOf("public void revealCurrentSessionRowIfShowing()", showIndex);
        Assert.assertTrue(nextMethodIndex > showIndex);
        String showBody = source.substring(showIndex, nextMethodIndex);
        Assert.assertTrue(showBody.contains("hideBrowserIfShowing()"));
    }

    @Test
    public void hideBrowserStepGatesOnVisibilityAndHidesBrowserOverlayWithoutForgettingItOrLoadingAnyUrl() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void hideBrowserIfShowing() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("isBrowserVisible()"));
        Assert.assertTrue(methodBody.contains("shouldHideBrowserOnOpen"));
        Assert.assertTrue(methodBody.contains("hideBrowserForSessionOverlay()"));
        Assert.assertFalse(methodBody.contains("showTerminal()"));
        Assert.assertFalse(methodBody.contains("loadUrl"));
        Assert.assertFalse(methodBody.contains("openUrlInNewTab"));
        Assert.assertFalse(methodBody.contains("showBrowser"));
    }

    @Test
    public void hideBrowserStepAlsoHidesProjectBrowserOverlayWhenVisible() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void hideBrowserIfShowing() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("getProjectBrowserOverlayController()"));
        Assert.assertTrue(methodBody.contains("projectBrowserOverlayController.isVisible()"));
        Assert.assertTrue(methodBody.contains("projectBrowserOverlayController.hide()"));
    }
}
