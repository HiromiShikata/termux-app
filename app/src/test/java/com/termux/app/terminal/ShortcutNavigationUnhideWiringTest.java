package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShortcutNavigationUnhideWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java";

    private String readSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void shortcutTapNavigationUnhidesTheTargetSessionBeforeSwitching() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);

        int methodIndex = source.indexOf("private void switchToShortcutSession(");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);

        Assert.assertTrue("tapping a shortcut must unhide the navigated session",
            methodBody.contains("unhideNavigatedShortcutSession(targetSession)"));

        int unhideMethodIndex = source.indexOf("private void unhideNavigatedShortcutSession(");
        Assert.assertTrue(unhideMethodIndex >= 0);
        int unhideMethodEnd = source.indexOf("\n    }", unhideMethodIndex);
        Assert.assertTrue(unhideMethodEnd > unhideMethodIndex);
        String unhideMethodBody = source.substring(unhideMethodIndex, unhideMethodEnd);

        Assert.assertTrue("the unhide must persist through the same disabled-session preference the filter reads",
            unhideMethodBody.contains(
                "OpenedSessionUnhider.unhideOpenedSession(preferences, terminalSession.mSessionName)"));
    }
}
