package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserProjectOverviewPageTest {

    @Test
    public void detectsUserProjectBoardUrl() {
        Assert.assertTrue(BrowserProjectOverviewPage.isOverviewUrl(
            "https://github.com/users/HiromiShikata/projects/48/views/16"));
    }

    @Test
    public void detectsOrgProjectBoardUrl() {
        Assert.assertTrue(BrowserProjectOverviewPage.isOverviewUrl(
            "https://github.com/orgs/some-org/projects/3"));
    }

    @Test
    public void detectsRepoProjectBoardUrl() {
        Assert.assertTrue(BrowserProjectOverviewPage.isOverviewUrl(
            "https://github.com/HiromiShikata/termux-app/projects/1"));
    }

    @Test
    public void rejectsIssueUrl() {
        Assert.assertFalse(BrowserProjectOverviewPage.isOverviewUrl(
            "https://github.com/HiromiShikata/termux-app/issues/272"));
    }

    @Test
    public void rejectsPullRequestUrl() {
        Assert.assertFalse(BrowserProjectOverviewPage.isOverviewUrl(
            "https://github.com/HiromiShikata/termux-app/pull/272"));
    }

    @Test
    public void rejectsNonGithubUrl() {
        Assert.assertFalse(BrowserProjectOverviewPage.isOverviewUrl("https://example.com/projects/1"));
    }

    @Test
    public void rejectsNullAndEmpty() {
        Assert.assertFalse(BrowserProjectOverviewPage.isOverviewUrl(null));
        Assert.assertFalse(BrowserProjectOverviewPage.isOverviewUrl(""));
    }
}
