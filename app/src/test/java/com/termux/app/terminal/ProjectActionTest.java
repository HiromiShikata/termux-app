package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ProjectActionTest {

    @Test
    public void resolvesOverviewUrlActionCaseInsensitively() {
        Assert.assertEquals(ProjectAction.OVERVIEW_URL, ProjectAction.fromTokenName("overviewUrl"));
        Assert.assertEquals(ProjectAction.OVERVIEW_URL, ProjectAction.fromTokenName("OVERVIEWURL"));
    }

    @Test
    public void resolvesTdpmConsoleUrlAction() {
        Assert.assertEquals(ProjectAction.TDPM_CONSOLE_URL, ProjectAction.fromTokenName("tdpmConsoleUrl"));
    }

    @Test
    public void resolvesNewIssueUrlAction() {
        Assert.assertEquals(ProjectAction.NEW_ISSUE_URL, ProjectAction.fromTokenName("newIssueUrl"));
    }

    @Test
    public void trimsWhitespaceWhenResolving() {
        Assert.assertEquals(ProjectAction.OVERVIEW_URL, ProjectAction.fromTokenName("  overviewUrl  "));
    }

    @Test
    public void returnsNullForUnknownAction() {
        Assert.assertNull(ProjectAction.fromTokenName("consoleUrl"));
    }

    @Test
    public void returnsNullForNullAction() {
        Assert.assertNull(ProjectAction.fromTokenName(null));
    }
}
