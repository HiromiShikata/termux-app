package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverlayRemovalTest {

    private Path moduleResource(String relativePath) {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app").resolve(relativePath);
    }

    private String readModuleResource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(moduleResource(relativePath)), StandardCharsets.UTF_8);
    }

    @Test
    public void projectBrowserOverlayControllerSourceIsDeleted() {
        Assert.assertFalse(Files.exists(moduleResource(
            "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java")));
        Assert.assertFalse(Files.exists(moduleResource(
            "src/main/java/com/termux/app/browser/ProjectUrlOpener.java")));
        Assert.assertFalse(Files.exists(moduleResource(
            "src/main/java/com/termux/app/browser/ProjectUrlRouter.java")));
    }

    @Test
    public void activityDoesNotReferenceProjectBrowserOverlay() throws IOException {
        String source = readModuleResource("src/main/java/com/termux/app/TermuxActivity.java");
        Assert.assertFalse(source.contains("ProjectBrowserOverlayController"));
        Assert.assertFalse(source.contains("getProjectBrowserOverlayController"));
        Assert.assertFalse(source.contains("REQUEST_PROJECT_BROWSER_FILE_CHOOSER"));
    }

    @Test
    public void sessionListEntryPointsDoNotReferenceProjectBrowserOverlay() throws IOException {
        String bottomSheet = readModuleResource(
            "src/main/java/com/termux/app/terminal/SessionListBottomSheetController.java");
        Assert.assertFalse(bottomSheet.contains("ProjectBrowserOverlayController"));
        Assert.assertFalse(bottomSheet.contains("getProjectBrowserOverlayController"));

        String sessionListController = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java");
        Assert.assertFalse(sessionListController.contains("ProjectBrowserOverlayController"));
        Assert.assertFalse(sessionListController.contains("getProjectBrowserOverlayController"));
    }

    @Test
    public void sessionListProjectHeaderIconsOpenInTheSessionBrowser() throws IOException {
        String sessionListController = readModuleResource(
            "src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java");
        int methodIndex = sessionListController.indexOf("private void openProjectUrlInNewTab(");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = sessionListController.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = sessionListController.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("getTermuxBrowserController()"));
        Assert.assertTrue(methodBody.contains("openUrlInNewTab("));
    }

    @Test
    public void activityLayoutHasNoProjectBrowserOverlay() throws IOException {
        String layout = readModuleResource("src/main/res/layout/activity_termux.xml");
        Assert.assertFalse(layout.contains("project_browser_overlay"));
        Assert.assertFalse(layout.contains("project_browser_footer"));
        Assert.assertFalse(layout.contains("project_browser_tab_strip"));
    }
}
