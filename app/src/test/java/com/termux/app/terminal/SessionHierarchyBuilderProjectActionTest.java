package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionHierarchyBuilderProjectActionTest {

    private static List<SessionHierarchyRow> sampleRows() {
        return Arrays.asList(
            SessionHierarchyRow.projectHeader("XMile",
                "https://overview.example/xmile",
                "https://console.example/xmile",
                "https://newissue.example/xmile"),
            SessionHierarchyRow.storyHeader("XMile Story"),
            SessionHierarchyRow.session(4),
            SessionHierarchyRow.session(5),
            SessionHierarchyRow.projectHeader("Umino",
                "https://overview.example/umino", null, null),
            SessionHierarchyRow.storyHeader("Umino Story"),
            SessionHierarchyRow.session(2));
    }

    @Test
    public void resolvesOverviewUrlForMatchingProjectCaseInsensitively() {
        Assert.assertEquals("https://overview.example/xmile",
            SessionHierarchyBuilder.projectActionUrl(sampleRows(), "xmile", ProjectAction.OVERVIEW_URL));
    }

    @Test
    public void resolvesTdpmConsoleUrlForMatchingProject() {
        Assert.assertEquals("https://console.example/xmile",
            SessionHierarchyBuilder.projectActionUrl(sampleRows(), "xmile", ProjectAction.TDPM_CONSOLE_URL));
    }

    @Test
    public void resolvesNewIssueUrlForMatchingProject() {
        Assert.assertEquals("https://newissue.example/xmile",
            SessionHierarchyBuilder.projectActionUrl(sampleRows(), "xmile", ProjectAction.NEW_ISSUE_URL));
    }

    @Test
    public void returnsNullWhenRequestedUrlIsAbsentInDefinition() {
        Assert.assertNull(
            SessionHierarchyBuilder.projectActionUrl(sampleRows(), "umino", ProjectAction.TDPM_CONSOLE_URL));
    }

    @Test
    public void returnsNullWhenProjectIsNotPresent() {
        Assert.assertNull(
            SessionHierarchyBuilder.projectActionUrl(sampleRows(), "secretary", ProjectAction.OVERVIEW_URL));
    }

    @Test
    public void firstSessionIndexForProjectReturnsTopSessionUnderHeader() {
        Assert.assertEquals(4,
            SessionHierarchyBuilder.firstSessionIndexForProject(sampleRows(), "xmile"));
    }

    @Test
    public void firstSessionIndexForProjectStopsAtNextProjectHeader() {
        Assert.assertEquals(2,
            SessionHierarchyBuilder.firstSessionIndexForProject(sampleRows(), "umino"));
    }

    @Test
    public void firstSessionIndexForProjectReturnsMinusOneWhenProjectAbsent() {
        Assert.assertEquals(-1,
            SessionHierarchyBuilder.firstSessionIndexForProject(sampleRows(), "secretary"));
    }

    @Test
    public void firstSessionIndexForProjectReturnsMinusOneWhenProjectHasNoSessions() {
        List<SessionHierarchyRow> rows = Arrays.asList(
            SessionHierarchyRow.projectHeader("XMile",
                "https://overview.example/xmile", null, null),
            SessionHierarchyRow.projectHeader("Umino",
                "https://overview.example/umino", null, null),
            SessionHierarchyRow.session(0));

        Assert.assertEquals(-1,
            SessionHierarchyBuilder.firstSessionIndexForProject(rows, "xmile"));
    }

    @Test
    public void firstSessionIndexForProjectMatchesNaProjectHeader() {
        List<SessionHierarchyRow> rows = Collections.singletonList(
            SessionHierarchyRow.projectHeader("N/A"));

        Assert.assertEquals(-1,
            SessionHierarchyBuilder.firstSessionIndexForProject(rows, "n/a"));
    }
}
