package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserFooterActionsTest {

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
    public void configureFooterActionsIsCalledOnceFromConstructor() throws IOException {
        String source = readControllerSource();
        int constructorStart = source.indexOf("public ProjectBrowserOverlayController(");
        int constructorEnd = source.indexOf("private void configureHeaderUrlMenu");
        Assert.assertTrue(constructorStart >= 0);
        Assert.assertTrue(constructorEnd > constructorStart);
        String constructorBody = source.substring(constructorStart, constructorEnd);
        Assert.assertTrue(constructorBody.contains("configureFooterActions();"));
        Assert.assertTrue(source.contains("private void configureFooterActions()"));
        int firstCall = source.indexOf("configureFooterActions();");
        int secondCall = source.indexOf("configureFooterActions();", firstCall + 1);
        Assert.assertEquals(-1, secondCall);
    }

    @Test
    public void wiresOverviewFooterIconToDesktopRoute() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_footer_overview_icon"));
        Assert.assertTrue(source.contains("route(mProjectOverviewUrl, BrowserViewMode.DESKTOP)"));
    }

    @Test
    public void wiresTdpmConsoleFooterIconToDesktopRoute() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_footer_tdpm_console_icon"));
        Assert.assertTrue(source.contains("route(mProjectTdpmConsoleUrl, BrowserViewMode.DESKTOP)"));
    }

    @Test
    public void wiresNewIssueFooterIconToDesktopRoute() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_footer_new_issue_icon"));
        Assert.assertTrue(source.contains("route(mProjectNewIssueUrl, BrowserViewMode.DESKTOP)"));
    }

    @Test
    public void hidesFooterIconWhenUrlIsAbsent() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "iconView.setVisibility(url == null || url.isEmpty() ? View.GONE : View.VISIBLE)"));
    }

    @Test
    public void setProjectContextStoresUrlsAndUpdatesVisibility() throws IOException {
        String source = readControllerSource();
        int setProjectContext = source.indexOf("public void setProjectContext(");
        Assert.assertTrue(setProjectContext >= 0);
        Assert.assertTrue(source.indexOf("mProjectOverviewUrl = overviewUrl", setProjectContext) > setProjectContext);
        Assert.assertTrue(source.indexOf("mProjectTdpmConsoleUrl = tdpmConsoleUrl", setProjectContext) > setProjectContext);
        Assert.assertTrue(source.indexOf("mProjectNewIssueUrl = newIssueUrl", setProjectContext) > setProjectContext);
        Assert.assertTrue(source.indexOf("updateFooterActionsVisibility()", setProjectContext) > setProjectContext);
    }
}
