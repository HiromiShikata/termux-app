package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverlayStaysOpenOnSessionAccessTest {

    private static final String SESSION_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String BOTTOM_SHEET_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String setCurrentSessionBody(String source) {
        int methodIndex = source.indexOf("public void setCurrentSession(TerminalSession session)");
        Assert.assertTrue(methodIndex >= 0);
        int bodyStart = source.indexOf('{', methodIndex);
        Assert.assertTrue(bodyStart > methodIndex);

        int depth = 0;
        for (int index = bodyStart; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, index + 1);
                }
            }
        }
        Assert.fail("setCurrentSession body not terminated");
        return "";
    }

    @Test
    public void setCurrentSessionDoesNotDismissProjectBrowserOverlay() throws IOException {
        String body = setCurrentSessionBody(readModuleResource(SESSION_CLIENT_RELATIVE_PATH));
        Assert.assertFalse(body.contains("getProjectBrowserOverlayController"));
        Assert.assertFalse(body.contains(".hide()"));
    }

    @Test
    public void sessionAccessChokePointDoesNotReferenceProjectBrowserDismissalHelper()
        throws IOException {
        String source = readModuleResource(SESSION_CLIENT_RELATIVE_PATH);
        Assert.assertFalse(source.contains("ProjectBrowserSessionDismissal"));
    }

    @Test
    public void openingSessionListBottomSheetStillHidesProjectBrowserOverlay() throws IOException {
        String source = readModuleResource(BOTTOM_SHEET_RELATIVE_PATH);
        int hideBrowserIndex = source.indexOf("private void hideBrowserIfShowing()");
        Assert.assertTrue(hideBrowserIndex >= 0);
        int controllerIndex =
            source.indexOf("getProjectBrowserOverlayController", hideBrowserIndex);
        int hideCallIndex =
            source.indexOf("projectBrowserOverlayController.hide()", hideBrowserIndex);
        int nextMethodIndex = source.indexOf("static boolean shouldHideBrowserOnOpen", hideBrowserIndex);
        Assert.assertTrue(controllerIndex > hideBrowserIndex);
        Assert.assertTrue(hideCallIndex > controllerIndex);
        Assert.assertTrue(hideCallIndex < nextMethodIndex);
    }
}
