package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShortcutNavigationProjectExpandWiringTest {

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
    public void shortcutTapNavigationExpandsTheTargetSessionProjectBeforeSwitching() throws IOException {
        String source = readSource(CONTROLLER_RELATIVE_PATH);

        int methodIndex = source.indexOf("private void switchToShortcutSession(");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);

        Assert.assertTrue("tapping a shortcut must expand the navigated session's collapsed project",
            methodBody.contains("expandNavigatedShortcutSessionProject(targetSession)"));

        int expandMethodIndex = source.indexOf("private void expandNavigatedShortcutSessionProject(");
        Assert.assertTrue(expandMethodIndex >= 0);
        int expandMethodEnd = source.indexOf("\n    }", expandMethodIndex);
        Assert.assertTrue(expandMethodEnd > expandMethodIndex);
        String expandMethodBody = source.substring(expandMethodIndex, expandMethodEnd);

        Assert.assertTrue("the expansion must route through the list controller that owns the collapsed set",
            expandMethodBody.contains(
                "listController.expandCollapsedProjectForSession(terminalSession.mSessionName)"));
    }
}
