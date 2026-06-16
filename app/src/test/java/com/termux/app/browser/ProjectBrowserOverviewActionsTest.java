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

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void wiresOpenAllButtonToBulkOpenWithNoLimit() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_open_all_tasks_button"));
        Assert.assertTrue(source.contains("mBulkOpenController.openDisplayedTaskUrls(0)"));
    }

    @Test
    public void wiresOpenFirstTenButtonToBulkOpenWithLimit() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("R.id.project_browser_open_first_ten_tasks_button"));
        Assert.assertTrue(source.contains(
            "mBulkOpenController.openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT)"));
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
}
