package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionListBottomSheetBrowserUntouchedWiringTest {

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

    private String methodBody(String source, String signature, String nextSignature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue(methodIndex >= 0);
        int nextMethodIndex = source.indexOf(nextSignature, methodIndex + signature.length());
        Assert.assertTrue(nextMethodIndex > methodIndex);
        return source.substring(methodIndex, nextMethodIndex);
    }

    @Test
    public void showDoesNotHideOrMutateTheBrowser() throws IOException {
        String showBody = methodBody(
            readControllerSource(), "public void show() {", "public void revealCurrentSessionRowIfShowing()");

        Assert.assertFalse(showBody.contains("hideBrowserIfShowing"));
        Assert.assertFalse(showBody.contains("hideBrowserForSessionOverlay"));
        Assert.assertFalse(showBody.contains("showTerminal"));
        Assert.assertFalse(showBody.contains("getTermuxBrowserController"));
    }

    @Test
    public void hideDoesNotHideOrMutateTheBrowser() throws IOException {
        String hideBody = methodBody(
            readControllerSource(), "public void hide() {", "private void applySessionInfoVisibilityForSheet(");

        Assert.assertFalse(hideBody.contains("hideBrowserForSessionOverlay"));
        Assert.assertFalse(hideBody.contains("showTerminal"));
        Assert.assertFalse(hideBody.contains("getTermuxBrowserController"));
    }

    @Test
    public void controllerNoLongerDeclaresTheHideBrowserOnOpenHelpers() throws IOException {
        String source = readControllerSource();

        Assert.assertFalse(source.contains("hideBrowserIfShowing"));
        Assert.assertFalse(source.contains("shouldHideBrowserOnOpen"));
        Assert.assertFalse(source.contains("hideBrowserForSessionOverlay"));
    }
}
