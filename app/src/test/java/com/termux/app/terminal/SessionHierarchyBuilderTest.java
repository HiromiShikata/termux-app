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
    public void dropsOrphanedProjectUrlSessionsWhilePlacingAdHocSessionsUnderNa() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/dropped", "manual-session"),
            entries, NA);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 2);
        assertProjectHeader(rows.get(2), "projectOne");
        assertStoryHeader(rows.get(3), "storyA");
        assertSession(rows.get(4), 0);
    }

    @Test
    public void omitsNaHeaderWhenEveryUnmatchedSessionIsAnOrphanedProjectUrl() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/dropped"), entries, NA);

        Assert.assertEquals(3, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
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

    @Test
    public void projectHeaderCarriesOverviewUrlFromEntriesThatProvideIt() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7"));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", rows.get(0).getOverviewUrl());
    }

    @Test
    public void projectHeaderHasNoOverviewUrlWhenEntriesProvideNone() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertNull(rows.get(0).getOverviewUrl());
    }

    @Test
    public void unmatchedNaProjectHeaderHasNoOverviewUrl() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7"));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("manual-session", "https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), NA);
        Assert.assertNull(rows.get(0).getOverviewUrl());
    }

    @Test
    public void projectHeaderCarriesTdpmConsoleUrlFromEntriesThatProvideIt() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7",
                "https://example.test/tdpm-console?k=TESTKEY"));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertEquals("https://example.test/tdpm-console?k=TESTKEY", rows.get(0).getTdpmConsoleUrl());
    }

    @Test
    public void projectHeaderHasNoTdpmConsoleUrlWhenEntriesProvideNone() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7"));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertNull(rows.get(0).getTdpmConsoleUrl());
    }

    @Test
    public void projectHeaderCarriesNewIssueUrlFromEntriesThatProvideIt() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7",
                "https://example.test/tdpm-console?k=TESTKEY",
                "https://example.test/new-issue?k=TESTKEY"));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertEquals("https://example.test/new-issue?k=TESTKEY", rows.get(0).getNewIssueUrl());
    }

    @Test
    public void projectHeaderHasNoNewIssueUrlWhenEntriesProvideNone() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a"),
                Collections.emptyMap(), "https://github.com/HiromiShikata/projects/7",
                "https://example.test/tdpm-console?k=TESTKEY"));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA);

        assertProjectHeader(rows.get(0), "projectOne");
        Assert.assertNull(rows.get(0).getNewIssueUrl());
    }

    @Test
    public void forcesSessionNamedInAlwaysNaSetIntoNaBucketEvenWhenItMatchesAProject() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>();
        alwaysNaSessionNames.add("https://example.test/a");

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA, alwaysNaSessionNames);

        Assert.assertEquals(2, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 0);
    }

    @Test
    public void keepsSessionsNotInAlwaysNaSetInTheirProjectWhileForcingNamedOnesToNa() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")),
            new SessionDefinitionEntry("projectOne", "storyB",
                Collections.singletonList("https://example.test/b")));

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>();
        alwaysNaSessionNames.add("https://example.test/a");

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, NA,
            alwaysNaSessionNames);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 0);
        assertProjectHeader(rows.get(2), "projectOne");
        assertStoryHeader(rows.get(3), "storyB");
        assertSession(rows.get(4), 1);
    }

    @Test
    public void leavesSessionsUnaffectedWhenTheirNamesAreNotInTheAlwaysNaSet() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>();
        alwaysNaSessionNames.add("some-other-session-name");

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, NA, alwaysNaSessionNames);

        Assert.assertEquals(3, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
    }

    @Test
    public void forcesAdHocNamedSessionIntoNaAlongsideOtherUnmatchedSessions() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        Set<String> alwaysNaSessionNames = new LinkedHashSet<>();
        alwaysNaSessionNames.add("https://example.test/a");

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, NA,
            alwaysNaSessionNames);

        Assert.assertEquals(3, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 0);
        assertSession(rows.get(2), 1);
    }

    @Test
    public void keepsANewlyCreatedBlankNamedSessionInTheNaBucket() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", null), entries, NA);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 1);
        assertProjectHeader(rows.get(2), "projectOne");
        assertStoryHeader(rows.get(3), "storyA");
        assertSession(rows.get(4), 0);
    }

    @Test
    public void keepsEveryDistinctUnmatchedSessionEvenWhenTheyShareTheSameName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "scratch", "scratch"), entries, NA);

        Assert.assertEquals(6, rows.size());
        assertProjectHeader(rows.get(0), NA);
        assertSession(rows.get(1), 1);
        assertSession(rows.get(2), 2);
        assertProjectHeader(rows.get(3), "projectOne");
        assertStoryHeader(rows.get(4), "storyA");
        assertSession(rows.get(5), 0);
    }

    @Test
    public void visibleSessionIndexesExtractsOnlySessionRowsInOrder() {
        List<SessionHierarchyRow> visibleRows = Arrays.asList(
            SessionHierarchyRow.projectHeader("projectOne"),
            SessionHierarchyRow.storyHeader("storyA"),
            SessionHierarchyRow.session(2),
            SessionHierarchyRow.session(5),
            SessionHierarchyRow.projectHeader("projectTwo"),
            SessionHierarchyRow.storyHeader("storyB"),
            SessionHierarchyRow.session(7));

        Assert.assertEquals(Arrays.asList(2, 5, 7),
            SessionHierarchyBuilder.visibleSessionIndexes(visibleRows));
    }

    @Test
    public void nextVisibleSessionIndexCyclesForwardWhenNothingIsCollapsed() {
        List<Integer> visible = Arrays.asList(0, 1, 2);

        Assert.assertEquals(1, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 0, true));
        Assert.assertEquals(2, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 1, true));
    }

    @Test
    public void nextVisibleSessionIndexCyclesBackwardWhenNothingIsCollapsed() {
        List<Integer> visible = Arrays.asList(0, 1, 2);

        Assert.assertEquals(1, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 2, false));
        Assert.assertEquals(0, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 1, false));
    }

    @Test
    public void nextVisibleSessionIndexWrapsAroundForwardAtTheEnd() {
        List<Integer> visible = Arrays.asList(0, 1, 2);

        Assert.assertEquals(0, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 2, true));
    }

    @Test
    public void nextVisibleSessionIndexWrapsAroundBackwardAtTheStart() {
        List<Integer> visible = Arrays.asList(0, 1, 2);

        Assert.assertEquals(2, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 0, false));
    }

    @Test
    public void nextVisibleSessionIndexSkipsCollapsedSessionsForward() {
        List<Integer> visible = Arrays.asList(0, 3, 4);

        Assert.assertEquals(3, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 0, true));
        Assert.assertEquals(0, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 4, true));
    }

    @Test
    public void nextVisibleSessionIndexSkipsCollapsedSessionsBackward() {
        List<Integer> visible = Arrays.asList(0, 3, 4);

        Assert.assertEquals(0, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 3, false));
        Assert.assertEquals(4, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 0, false));
    }

    @Test
    public void nextVisibleSessionIndexReturnsNegativeOneWhenAllProjectsCollapsed() {
        Assert.assertEquals(-1,
            SessionHierarchyBuilder.nextVisibleSessionIndex(Collections.emptyList(), 0, true));
        Assert.assertEquals(-1,
            SessionHierarchyBuilder.nextVisibleSessionIndex(Collections.emptyList(), 0, false));
    }

    @Test
    public void nextVisibleSessionIndexFromCollapsedCurrentPicksNearestForward() {
        List<Integer> visible = Arrays.asList(0, 1, 5, 6);

        Assert.assertEquals(5, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 3, true));
    }

    @Test
    public void nextVisibleSessionIndexFromCollapsedCurrentPicksNearestBackward() {
        List<Integer> visible = Arrays.asList(0, 1, 5, 6);

        Assert.assertEquals(1, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 3, false));
    }

    @Test
    public void nextVisibleSessionIndexFromCollapsedCurrentWrapsForwardWhenNoneGreater() {
        List<Integer> visible = Arrays.asList(0, 1, 2);

        Assert.assertEquals(0, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 9, true));
    }

    @Test
    public void nextVisibleSessionIndexFromCollapsedCurrentWrapsBackwardWhenNoneSmaller() {
        List<Integer> visible = Arrays.asList(3, 4, 5);

        Assert.assertEquals(5, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 1, false));
    }

    @Test
    public void nextVisibleSessionIndexReturnsTheSoleVisibleSessionWhenItIsAlsoCurrent() {
        List<Integer> visible = Collections.singletonList(2);

        Assert.assertEquals(2, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 2, true));
        Assert.assertEquals(2, SessionHierarchyBuilder.nextVisibleSessionIndex(visible, 2, false));
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
