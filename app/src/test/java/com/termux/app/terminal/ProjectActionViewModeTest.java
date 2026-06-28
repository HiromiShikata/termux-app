package com.termux.app.terminal;

import com.termux.app.browser.BrowserViewMode;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProjectActionViewModeTest {

    @Test
    public void overviewBoardOpensInMobileViewMode() {
        Assert.assertEquals(BrowserViewMode.MOBILE,
            TermuxSessionsListViewController.projectActionViewMode(ProjectAction.OVERVIEW_URL));
    }

    @Test
    public void tdpmConsoleOpensInMobileViewMode() {
        Assert.assertEquals(BrowserViewMode.MOBILE,
            TermuxSessionsListViewController.projectActionViewMode(ProjectAction.TDPM_CONSOLE_URL));
    }

    @Test
    public void newIssueOpensInDesktopViewMode() {
        Assert.assertEquals(BrowserViewMode.DESKTOP,
            TermuxSessionsListViewController.projectActionViewMode(ProjectAction.NEW_ISSUE_URL));
    }
}
