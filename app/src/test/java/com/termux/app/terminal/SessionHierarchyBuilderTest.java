package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionHierarchyBuilderTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    @Test
    public void buildsProjectThenStoryThenSessionRowsForASingleStory() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        Assert.assertEquals(3, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
    }

    @Test
    public void groupsMultipleStoriesUnderTheSameProjectWithASingleProjectHeader() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")),
            new SessionDefinitionEntry("projectOne", "storyB",
                Collections.singletonList("https://example.test/b")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, NA);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
        assertStoryHeader(rows.get(3), "storyB");
        assertSession(rows.get(4), 1);
    }

    @Test
    public void emitsOneProjectHeaderPerProjectInFirstAppearanceOrder() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, NA);

        Assert.assertEquals(6, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
        assertProjectHeader(rows.get(3), "projectTwo");
        assertStoryHeader(rows.get(4), "storyB");
        assertSession(rows.get(5), 1);
    }

    @Test
    public void groupsMultipleSessionsOfTheSameStoryUnderOneStoryHeader() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);

        Assert.assertEquals(4, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
        assertSession(rows.get(3), 1);
    }

    @Test
    public void placesUnmatchedSessionsUnderAnNaGroupAtTheTopWithoutAStoryHeader() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, NA);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 1);
        assertProjectHeader(rows.get(2), "projectOne");
        assertStoryHeader(rows.get(3), "storyA");
        assertSession(rows.get(4), 0);
    }

    @Test
    public void ordersProjectSectionByDataStoryOrderWithTopStoryFirstRegardlessOfSessionOrder() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyTop",
                Collections.singletonList("https://example.test/top")),
            new SessionDefinitionEntry("projectOne", "storyBottom",
                Collections.singletonList("https://example.test/bottom")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/bottom", "https://example.test/top"), entries, NA);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyTop");
        assertSession(rows.get(2), 1);
        assertStoryHeader(rows.get(3), "storyBottom");
        assertSession(rows.get(4), 0);
    }

    @Test
    public void deduplicatesSameNameSessionsToASingleRow() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/a"), entries, NA);

        Assert.assertEquals(3, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
    }

    @Test
    public void naBucketSortsToTheVeryTopAboveTheProjectSection() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, NA);

        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 1);
    }

    @Test
    public void firstSessionIndexReturnsTheTopVisibleSessionInTheNaBucket() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, NA);

        Assert.assertEquals(1, SessionHierarchyBuilder.firstSessionIndex(rows));
    }

    @Test
    public void firstSessionIndexReturnsTheTopVisibleProjectSessionWhenNoUnmatchedSessionsExist() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyTop",
                Collections.singletonList("https://example.test/top")),
            new SessionDefinitionEntry("projectOne", "storyBottom",
                Collections.singletonList("https://example.test/bottom")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/bottom", "https://example.test/top"), entries, NA);

        Assert.assertEquals(1, SessionHierarchyBuilder.firstSessionIndex(rows));
    }

    @Test
    public void firstSessionIndexReturnsNegativeOneWhenNoSessionRowsExist() {
        Assert.assertEquals(-1, SessionHierarchyBuilder.firstSessionIndex(Collections.emptyList()));
    }

    @Test
    public void fallsBackToAFlatSessionListWhenEntriesAreEmpty() {
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"),
            Collections.emptyList(), NA);

        Assert.assertEquals(2, rows.size());
        assertSession(rows.get(0), 0);
        assertSession(rows.get(1), 1);
    }

    @Test
    public void returnsNoRowsWhenThereAreNoSessions() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.emptyList(), entries, NA);

        Assert.assertTrue(rows.isEmpty());
    }

    @Test
    public void filterCollapsedProjectsReturnsAllRowsWhenNoProjectIsCollapsed() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        List<SessionHierarchyRow> visibleRows =
            builder.filterCollapsedProjects(rows, Collections.emptySet());

        Assert.assertEquals(rows, visibleRows);
    }

    @Test
    public void filterCollapsedProjectsHidesStoryHeadersAndSessionsOfACollapsedProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b")));
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, NA);

        Set<String> collapsed = new LinkedHashSet<>();
        collapsed.add("projectOne");
        List<SessionHierarchyRow> visibleRows = builder.filterCollapsedProjects(rows, collapsed);

        Assert.assertEquals(4, visibleRows.size());
        assertProjectHeader(visibleRows.get(0), "projectOne");
        assertProjectHeader(visibleRows.get(1), "projectTwo");
        assertStoryHeader(visibleRows.get(2), "storyB");
        assertSession(visibleRows.get(3), 1);
    }

    @Test
    public void filterCollapsedProjectsKeepsTheCollapsedProjectHeaderVisible() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);

        Set<String> collapsed = new LinkedHashSet<>();
        collapsed.add("projectOne");
        List<SessionHierarchyRow> visibleRows = builder.filterCollapsedProjects(rows, collapsed);

        Assert.assertEquals(1, visibleRows.size());
        assertProjectHeader(visibleRows.get(0), "projectOne");
    }

    @Test
    public void filterCollapsedProjectsCollapsesTheUnmatchedNaProjectGroup() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, NA);

        Set<String> collapsed = new LinkedHashSet<>();
        collapsed.add(NA);
        List<SessionHierarchyRow> visibleRows = builder.filterCollapsedProjects(rows, collapsed);

        Assert.assertEquals(4, visibleRows.size());
        assertProjectHeader(visibleRows.get(0), NA);
        assertProjectHeader(visibleRows.get(1), "projectOne");
        assertStoryHeader(visibleRows.get(2), "storyA");
        assertSession(visibleRows.get(3), 0);
    }

    private void assertProjectHeader(SessionHierarchyRow row, String expectedLabel) {
        Assert.assertEquals(SessionHierarchyRow.Type.PROJECT_HEADER, row.getType());
        Assert.assertTrue(row.isHeader());
        Assert.assertEquals(expectedLabel, row.getLabel());
    }

    private void assertStoryHeader(SessionHierarchyRow row, String expectedLabel) {
        Assert.assertEquals(SessionHierarchyRow.Type.STORY_HEADER, row.getType());
        Assert.assertTrue(row.isHeader());
        Assert.assertEquals(expectedLabel, row.getLabel());
    }

    private void assertSession(SessionHierarchyRow row, int expectedSessionIndex) {
        Assert.assertEquals(SessionHierarchyRow.Type.SESSION, row.getType());
        Assert.assertFalse(row.isHeader());
        Assert.assertEquals(expectedSessionIndex, row.getSessionIndex());
    }
}
