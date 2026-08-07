package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionHierarchyBuilderSessionCountTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    @Test
    public void totalSessionCountCountsEverySessionRowAcrossProjects() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2",
                "https://example.test/b1"),
            entries, NA);

        Assert.assertEquals("three story sessions plus the project-manager row each of the two projects"
            + " draws", 5, SessionHierarchyBuilder.totalSessionCount(rows));
    }

    @Test
    public void totalSessionCountForFlatListEqualsSessionCount() {
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("alpha", "beta", "gamma"),
            Collections.emptyList(), NA);

        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(rows));
    }

    @Test
    public void totalSessionCountIsZeroForEmptyList() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.emptyList(), Collections.emptyList(), NA);

        Assert.assertEquals(0, SessionHierarchyBuilder.totalSessionCount(rows));
    }

    @Test
    public void sessionCountByProjectLabelCountsSessionsPerProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2",
                "https://example.test/b1"),
            entries, NA);

        Map<String, Integer> countByProject = SessionHierarchyBuilder.sessionCountByProjectLabel(rows);
        Assert.assertEquals("two story sessions plus the project-manager row",
            Integer.valueOf(3), countByProject.get("projectOne"));
        Assert.assertEquals("one story session plus the project-manager row",
            Integer.valueOf(2), countByProject.get("projectTwo"));
    }

    @Test
    public void sessionCountByProjectLabelCountsSessionsAcrossMultipleStoriesInSameProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("projectOne", "storyB",
                Arrays.asList("https://example.test/b1", "https://example.test/b2")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/b1",
                "https://example.test/b2"),
            entries, NA);

        Map<String, Integer> countByProject = SessionHierarchyBuilder.sessionCountByProjectLabel(rows);
        Assert.assertEquals("three story sessions across two stories plus the project-manager row",
            Integer.valueOf(4), countByProject.get("projectOne"));
    }

    @Test
    public void sessionCountByProjectLabelCountsUnmatchedSessionsUnderNaProject() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a1")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a1", "orphan-one", "orphan-two"),
            entries, NA);

        Map<String, Integer> countByProject = SessionHierarchyBuilder.sessionCountByProjectLabel(rows);
        Assert.assertEquals(Integer.valueOf(2), countByProject.get(NA));
        Assert.assertEquals("one story session plus the project-manager row",
            Integer.valueOf(2), countByProject.get("projectOne"));
    }

    @Test
    public void countsKeepEveryDefinedSessionWhenOnlySomeOfThemAreLive() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        List<SessionHierarchyRow> before = builder.build(
            Collections.singletonList("https://example.test/a1"), entries, NA);
        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(before));
        Assert.assertEquals("both defined story sessions plus the project-manager row",
            Integer.valueOf(3),
            SessionHierarchyBuilder.sessionCountByProjectLabel(before).get("projectOne"));

        List<SessionHierarchyRow> after = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);
        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(after));
        Assert.assertEquals("the count does not change when the second session becomes live",
            Integer.valueOf(3),
            SessionHierarchyBuilder.sessionCountByProjectLabel(after).get("projectOne"));
    }

    @Test
    public void countsKeepADefinedSessionAfterItsLiveSessionObjectIsReleased() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        List<SessionHierarchyRow> before = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);
        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(before));

        List<SessionHierarchyRow> after = builder.build(
            Collections.singletonList("https://example.test/a1"), entries, NA);
        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(after));
        Assert.assertEquals("both defined story sessions plus the project-manager row survive the"
            + " release of one live session object", Integer.valueOf(3),
            SessionHierarchyBuilder.sessionCountByProjectLabel(after).get("projectOne"));
    }

    @Test
    public void pendingCallSessionCountCountsOnlySessionsWhoseNameIsPendingACall() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a1", "https://example.test/b1"));

        Assert.assertEquals(2, SessionHierarchyBuilder.pendingCallSessionCount(
            rows, sessionNames, pendingCallSessionNames));
    }

    @Test
    public void pendingCallSessionCountIsZeroWhenNoSessionIsPendingACall() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);

        Assert.assertEquals(0, SessionHierarchyBuilder.pendingCallSessionCount(
            rows, sessionNames, Collections.emptySet()));
    }

    @Test
    public void pendingCallSessionCountByProjectLabelCountsCallsPerProject() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a1", "https://example.test/b1"));

        Map<String, Integer> pendingByProject = SessionHierarchyBuilder.pendingCallSessionCountByProjectLabel(
            rows, sessionNames, pendingCallSessionNames);
        Assert.assertEquals(Integer.valueOf(1), pendingByProject.get("projectOne"));
        Assert.assertEquals(Integer.valueOf(1), pendingByProject.get("projectTwo"));
    }

    @Test
    public void pendingCallSessionCountByProjectLabelReportsZeroForProjectsWithoutACall() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> pendingCallSessionNames = new HashSet<>(
            Collections.singletonList("https://example.test/a1"));

        Map<String, Integer> pendingByProject = SessionHierarchyBuilder.pendingCallSessionCountByProjectLabel(
            rows, sessionNames, pendingCallSessionNames);
        Assert.assertEquals(Integer.valueOf(1), pendingByProject.get("projectOne"));
        Assert.assertEquals(Integer.valueOf(0), pendingByProject.get("projectTwo"));
    }

    @Test
    public void filterCollapsedProjectSessionsExcludesCollapsedSessionsFromTheTotalAndNotificationCount() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a1", "https://example.test/b1"));

        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(rows, collapsed);

        Assert.assertEquals("only the expanded project's story session and its project-manager row are"
            + " counted", 2, SessionHierarchyBuilder.totalSessionCount(countedRows));
        Assert.assertEquals(1, SessionHierarchyBuilder.pendingCallSessionCount(
            countedRows, sessionNames, pendingCallSessionNames));
    }

    @Test
    public void filterCollapsedProjectSessionsIncludesExpandedProjectSessionsInTheCounts() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Arrays.asList("https://example.test/b1", "https://example.test/b2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/b1", "https://example.test/b2");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));

        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(rows, collapsed);

        Map<String, Integer> countByProject =
            SessionHierarchyBuilder.sessionCountByProjectLabel(countedRows);
        Assert.assertEquals(Integer.valueOf(0), countByProject.get("collapsedProject"));
        Assert.assertEquals("two story sessions plus the project-manager row",
            Integer.valueOf(3), countByProject.get("expandedProject"));
        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(countedRows));
    }

    @Test
    public void filterCollapsedProjectSessionsReturnsAllRowsWhenNoProjectIsCollapsed() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a1")));
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a1"), entries, NA);

        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(rows, Collections.emptySet());

        Assert.assertEquals(rows, countedRows);
    }

    @Test
    public void hiddenSessionExclusionStillAppliesAlongsideCollapsedProjectExclusion() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Arrays.asList("https://example.test/b1", "https://example.test/b2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/b1", "https://example.test/b2");
        List<SessionHierarchyRow> allRows = builder.build(sessionNames, entries, NA);
        Set<String> hiddenSessionNames = new HashSet<>(
            Collections.singletonList("https://example.test/b2"));
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));

        List<SessionHierarchyRow> renderedRows =
            SessionHierarchyBuilder.filterHiddenSessions(allRows, sessionNames, hiddenSessionNames);
        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(renderedRows, collapsed);

        Map<String, Integer> countByProject =
            SessionHierarchyBuilder.sessionCountByProjectLabel(countedRows);
        Assert.assertEquals(Integer.valueOf(0), countByProject.get("collapsedProject"));
        Assert.assertEquals("the remaining story session plus the project-manager row",
            Integer.valueOf(2), countByProject.get("expandedProject"));
        Assert.assertEquals(2, SessionHierarchyBuilder.totalSessionCount(countedRows));
    }
}
