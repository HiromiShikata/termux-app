package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerSessionOverlayHideWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void showTerminalForgetsRememberedVisibilityForTheCurrentSession() throws IOException {
        String showTerminalBody = methodBody(readControllerSource(), "public void showTerminal() {");

        Assert.assertTrue(showTerminalBody.contains(
            "mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mCurrentSessionName, false)"));
        Assert.assertTrue(showTerminalBody.contains("hideBrowserViews()"));
    }

    @Test
    public void onSessionChangedDoesNotOverwriteTheLeavingSessionRememberedVisibility() throws IOException {
        String onSessionChangedBody = methodBody(
            readControllerSource(), "public void onSessionChanged(@Nullable TerminalSession session) {");

        Assert.assertFalse(onSessionChangedBody.contains(
            "setBrowserVisible(mCurrentSessionHandle, mCurrentSessionName, mBrowserVisible)"));
    }
}
