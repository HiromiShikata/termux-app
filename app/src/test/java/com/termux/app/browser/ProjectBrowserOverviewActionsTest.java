package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectBrowserOverviewActionsTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/ProjectBrowserOverlayController.java";

    private static final String STRINGS_RELATIVE_PATH =
        "src/main/res/values/strings.xml";

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private String readModuleFile(String moduleRelativePath) throws IOException {
        Path moduleRelative = Paths.get(moduleRelativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(moduleRelativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String readControllerSource() throws IOException {
        return readModuleFile(CONTROLLER_RELATIVE_PATH);
    }

    @Test
    public void wiresOpenAllButtonToBulkOpenWithNoLimit() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_open_all_tasks_button"));
        Assert.assertTrue(source.contains("openDisplayedTaskUrls(0)"));
    }

    @Test
    public void wiresOpenFirstTenButtonToBulkOpenWithLimit() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_open_first_ten_tasks_button"));
        Assert.assertTrue(source.contains(
            "openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT)"));
        Assert.assertTrue(source.contains(
            "mBulkOpenController.openDisplayedTaskUrls(displayedWebView, limit)"));
    }

    @Test
    public void visibilityIsGatedOnOverviewUrlAndOverlayVisible() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains(
            "mVisible && BrowserProjectOverviewPage.isOverviewUrl(mCurrentUrl)"));
    }

    @Test
    public void visibilityUpdatesOnPageLifecycleAndShowAndHide() throws IOException {
        String source = readControllerSource();
        int onPageStarted = source.indexOf("onPageStarted");
        int onPageFinished = source.indexOf("onPageFinished");
        Assert.assertTrue(onPageStarted >= 0);
        Assert.assertTrue(onPageFinished >= 0);
        Assert.assertTrue(source.indexOf("updateOverviewActionsVisibility", onPageStarted) > onPageStarted);
        Assert.assertTrue(source.indexOf("updateOverviewActionsVisibility", onPageFinished) > onPageFinished);
        int show = source.indexOf("private void show()");
        int hide = source.indexOf("public void hide()");
        Assert.assertTrue(source.indexOf("updateOverviewActionsVisibility", show) > show);
        Assert.assertTrue(source.indexOf("updateOverviewActionsVisibility", hide) > hide);
    }

    @Test
    public void tracksCurrentUrlOnOpenProjectUrl() throws IOException {
        String source = readControllerSource();
        int openProjectUrl = source.indexOf("public void openProjectUrl");
        Assert.assertTrue(openProjectUrl >= 0);
        Assert.assertTrue(source.indexOf("mCurrentUrl = url", openProjectUrl) > openProjectUrl);
    }

    @Test
    public void usesCompactOverviewActionLabels() throws IOException {
        String strings = readModuleFile(STRINGS_RELATIVE_PATH);
        Assert.assertTrue(strings.contains(
            "<string name=\"action_browser_open_all_tasks\">All</string>"));
        Assert.assertTrue(strings.contains(
            "<string name=\"action_browser_open_first_ten_tasks\">10</string>"));
    }

    @Test
    public void overviewActionButtonsKeepTappableTouchTarget() throws IOException {
        String layout = readModuleFile(LAYOUT_RELATIVE_PATH);
        int openAllButton = layout.indexOf("@+id/project_browser_open_all_tasks_button");
        int openFirstTenButton = layout.indexOf("@+id/project_browser_open_first_ten_tasks_button");
        Assert.assertTrue(openAllButton >= 0);
        Assert.assertTrue(openFirstTenButton >= 0);
        int openAllEnd = layout.indexOf("/>", openAllButton);
        int openFirstTenEnd = layout.indexOf("/>", openFirstTenButton);
        String openAllAttributes = layout.substring(openAllButton, openAllEnd);
        String openFirstTenAttributes = layout.substring(openFirstTenButton, openFirstTenEnd);
        Assert.assertTrue(openAllAttributes.contains("android:minHeight=\"36dp\""));
        Assert.assertTrue(openAllAttributes.contains("android:minWidth=\"48dp\""));
        Assert.assertTrue(openFirstTenAttributes.contains("android:minHeight=\"36dp\""));
        Assert.assertTrue(openFirstTenAttributes.contains("android:minWidth=\"48dp\""));
    }
}
