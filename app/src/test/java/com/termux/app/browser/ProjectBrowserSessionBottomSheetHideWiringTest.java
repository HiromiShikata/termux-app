package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserSessionBottomSheetHideWiringTest {

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
    public void showInvokesTheSessionBottomSheetHideStep() throws IOException {
        String source = readControllerSource();
        int showIndex = source.indexOf("private void show() {");
        Assert.assertTrue(showIndex >= 0);
        int nextMethodIndex = source.indexOf("private void hideSessionBottomSheetIfShowing()", showIndex);
        Assert.assertTrue(nextMethodIndex > showIndex);
        String showBody = source.substring(showIndex, nextMethodIndex);
        Assert.assertTrue(showBody.contains("hideSessionBottomSheetIfShowing()"));
    }

    @Test
    public void hideSessionBottomSheetStepClosesTheSheetViaTheNullSafeHelper() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void hideSessionBottomSheetIfShowing() {");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("getSessionListBottomSheetController()"));
        Assert.assertTrue(methodBody.contains("SessionListBottomSheetController.hideIfPresent"));
    }

    @Test
    public void openProjectUrlPathReachesShow() throws IOException {
        String source = readControllerSource();
        int openIndex = source.indexOf("public void openProjectUrl(");
        Assert.assertTrue(openIndex >= 0);
        int openEnd = source.indexOf("\n    }", openIndex);
        Assert.assertTrue(openEnd > openIndex);
        String openBody = source.substring(openIndex, openEnd);
        Assert.assertTrue(openBody.contains("show();"));
    }
}
