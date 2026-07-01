package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionBrowserTabBarActionButtonsWiringTest {

    private static final String LAYOUT_RELATIVE_PATH =
        "src/main/res/layout/activity_termux.xml";

    private static final String SESSION_CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void tabBarContainsTheThreeFixedActionButtons() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        Assert.assertTrue(layout.contains("@+id/browser_tab_bar_overview_button"));
        Assert.assertTrue(layout.contains("@+id/browser_tab_bar_tdpm_console_button"));
        Assert.assertTrue(layout.contains("@+id/browser_tab_bar_new_issue_button"));
    }

    @Test
    public void addTaskButtonUsesADistinctIconFromTheNewTabButton() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        String addTaskSrc = drawableSrcForButton(layout, "@+id/browser_tab_bar_new_issue_button");
        String newTabSrc = drawableSrcForButton(layout, "@+id/browser_new_tab_button");
        Assert.assertNotNull("Add Task button must declare android:src", addTaskSrc);
        Assert.assertNotNull("New-tab button must declare android:src", newTabSrc);
        Assert.assertNotEquals(
            "Add Task button must not reuse the new-tab plus icon; it needs a distinct create-new-task glyph",
            newTabSrc, addTaskSrc);
        Assert.assertEquals("@drawable/ic_add_task", addTaskSrc);
    }

    private String drawableSrcForButton(String layout, String buttonId) {
        int idIndex = layout.indexOf(buttonId);
        if (idIndex < 0) {
            return null;
        }
        int elementEnd = layout.indexOf("/>", idIndex);
        if (elementEnd < 0) {
            return null;
        }
        String element = layout.substring(idIndex, elementEnd);
        String marker = "android:src=\"";
        int srcIndex = element.indexOf(marker);
        if (srcIndex < 0) {
            return null;
        }
        int valueStart = srcIndex + marker.length();
        int valueEnd = element.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return element.substring(valueStart, valueEnd);
    }

    @Test
    public void fixedActionButtonsArePinnedRightOfTheScrollingTabStrip() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int tabBarIndex = layout.indexOf("@+id/browser_tab_bar");
        int scrollIndex = layout.indexOf("@+id/browser_tab_strip_scroll");
        int actionsIndex = layout.indexOf("@+id/browser_tab_bar_project_actions");
        Assert.assertTrue(tabBarIndex >= 0);
        Assert.assertTrue(scrollIndex > tabBarIndex);
        Assert.assertTrue("Fixed action buttons must appear after the scrolling tab strip",
            actionsIndex > scrollIndex);
    }

    @Test
    public void scrollingTabStripTakesRemainingWidthSoFixedButtonsStayPinnedAtTheEnd() throws IOException {
        String layout = readModuleResource(LAYOUT_RELATIVE_PATH);
        int scrollIndex = layout.indexOf("@+id/browser_tab_strip_scroll");
        int actionsIndex = layout.indexOf("@+id/browser_tab_bar_project_actions");
        String scrollBlock = layout.substring(scrollIndex, actionsIndex);
        Assert.assertTrue("Scroll view must weight-fill so the action buttons stay at the right edge",
            scrollBlock.contains("android:layout_weight=\"1\""));
    }

    @Test
    public void sessionControllerBindsTheTabBarActionButtonsController() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        Assert.assertTrue(source.contains("new BrowserProjectActionButtonsController("));
        Assert.assertTrue(source.contains("R.id.browser_tab_bar_overview_button"));
        Assert.assertTrue(source.contains("R.id.browser_tab_bar_tdpm_console_button"));
        Assert.assertTrue(source.contains("R.id.browser_tab_bar_new_issue_button"));
        Assert.assertTrue(source.contains("this::openProjectActionUrl"));
    }

    @Test
    public void sessionControllerRefreshesActionButtonsOnSessionChangeAndShow() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int onSessionChangedIndex = source.indexOf("public void onSessionChanged(");
        Assert.assertTrue(onSessionChangedIndex >= 0);
        int onSessionChangedEnd = source.indexOf("private void restoreSessionVisibility", onSessionChangedIndex);
        Assert.assertTrue(onSessionChangedEnd > onSessionChangedIndex);
        Assert.assertTrue(source.substring(onSessionChangedIndex, onSessionChangedEnd)
            .contains("updateProjectActionButtons()"));

        int showBrowserIndex = source.indexOf("public void showBrowser()");
        Assert.assertTrue(showBrowserIndex >= 0);
        int showBrowserEnd = source.indexOf("private void dismissSoftKeyboardForBrowser", showBrowserIndex);
        Assert.assertTrue(showBrowserEnd > showBrowserIndex);
        Assert.assertTrue(source.substring(showBrowserIndex, showBrowserEnd)
            .contains("updateProjectActionButtons()"));
    }

    @Test
    public void sessionControllerResolvesActionUrlsFromCurrentSessionProjectDefinition() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        Assert.assertTrue(source.contains("new BrowserProjectActionUrlResolver("));
        Assert.assertTrue(source.contains(
            "mProjectActionUrlResolver.resolveForSessionName(mCurrentSessionName)"));
    }

    @Test
    public void newTabPromptShowsUrlDialogForEverySessionWithoutManagerSessionSpecialBranch() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int promptNewTabIndex = source.indexOf("public void promptNewTab()");
        Assert.assertTrue(promptNewTabIndex >= 0);
        int promptNewTabEnd = source.indexOf("private static String normalizeUrl", promptNewTabIndex);
        Assert.assertTrue(promptNewTabEnd > promptNewTabIndex);
        String promptNewTabBody = source.substring(promptNewTabIndex, promptNewTabEnd);
        Assert.assertTrue("New-tab action must open the URL-input dialog for every session",
            promptNewTabBody.contains("TextInputDialogUtils.textInput("));
        Assert.assertFalse("New-tab action must not branch on manager-session overview URLs",
            promptNewTabBody.contains("ProjectManagerOverviewUrlResolver"));
        Assert.assertFalse("New-tab action must not branch on manager-session overview URLs",
            promptNewTabBody.contains("resolveOverviewUrlForManagerSessionName"));
        Assert.assertFalse("Controller must not retain a manager-session overview resolver",
            source.contains("BrowserProjectManagerOverviewUrlResolver"));
    }

    @Test
    public void sessionChangePreloadsProjectOverviewTabWithoutManagerSessionSpecialBranch() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int onSessionChangedIndex = source.indexOf("public void onSessionChanged(");
        Assert.assertTrue(onSessionChangedIndex >= 0);
        int onSessionChangedEnd = source.indexOf("private void restoreSessionVisibility", onSessionChangedIndex);
        Assert.assertTrue(onSessionChangedEnd > onSessionChangedIndex);
        String onSessionChangedBody = source.substring(onSessionChangedIndex, onSessionChangedEnd);
        Assert.assertTrue("Session change must preload the project overview tab for non-URL session names",
            onSessionChangedBody.contains("preloadProjectOverviewTabForSession("));

        int preloadIndex = source.indexOf("private void preloadProjectOverviewTabForSession(");
        Assert.assertTrue(preloadIndex >= 0);
        int preloadEnd = source.indexOf("private void", preloadIndex
            + "private void preloadProjectOverviewTabForSession(".length());
        Assert.assertTrue(preloadEnd > preloadIndex);
        String preloadBody = source.substring(preloadIndex, preloadEnd);
        Assert.assertTrue("Preload decision must go through the shared overview-preload resolver",
            preloadBody.contains("BrowserSessionOverviewPreload.resolvePreloadUrl"));
        Assert.assertFalse("Preload must not branch on manager-session names",
            preloadBody.contains("ManagerSession"));
        Assert.assertFalse("Preload must not branch on PM session type",
            preloadBody.contains("isManagerSession"));
    }

    @Test
    public void projectActionButtonVisibilityDependsOnOverviewUrlNotSessionType() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int visibilityIndex = source.indexOf("private void updateProjectOverviewActionsVisibility()");
        Assert.assertTrue(visibilityIndex >= 0);
        int visibilityEnd = source.indexOf("private void", visibilityIndex
            + "private void updateProjectOverviewActionsVisibility()".length());
        Assert.assertTrue(visibilityEnd > visibilityIndex);
        String visibilityBody = source.substring(visibilityIndex, visibilityEnd);
        Assert.assertTrue("Action-icon visibility must be gated on the overview URL",
            visibilityBody.contains("BrowserProjectOverviewPage.isOverviewUrl"));
        Assert.assertFalse("Action-icon visibility must not branch on manager-session names",
            visibilityBody.contains("ManagerSession"));
        Assert.assertFalse("Action-icon visibility must not branch on PM session type",
            visibilityBody.contains("isManagerSession"));
    }
}
