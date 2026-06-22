package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

        Assert.assertEquals(3, SessionHierarchyBuilder.totalSessionCount(rows));
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
        Assert.assertEquals(Integer.valueOf(2), countByProject.get("projectOne"));
        Assert.assertEquals(Integer.valueOf(1), countByProject.get("projectTwo"));
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
        Assert.assertEquals(Integer.valueOf(3), countByProject.get("projectOne"));
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
        Assert.assertEquals(Integer.valueOf(1), countByProject.get("projectOne"));
    }

    @Test
    public void countsReflectAddedSessionWhenSessionSetGrows() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        List<SessionHierarchyRow> before = builder.build(
            Collections.singletonList("https://example.test/a1"), entries, NA);
        Assert.assertEquals(1, SessionHierarchyBuilder.totalSessionCount(before));
        Assert.assertEquals(Integer.valueOf(1),
            SessionHierarchyBuilder.sessionCountByProjectLabel(before).get("projectOne"));

        List<SessionHierarchyRow> after = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);
        Assert.assertEquals(2, SessionHierarchyBuilder.totalSessionCount(after));
        Assert.assertEquals(Integer.valueOf(2),
            SessionHierarchyBuilder.sessionCountByProjectLabel(after).get("projectOne"));
    }

    @Test
    public void countsReflectRemovedSessionWhenSessionSetShrinks() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));

        List<SessionHierarchyRow> before = builder.build(
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, NA);
        Assert.assertEquals(2, SessionHierarchyBuilder.totalSessionCount(before));

        List<SessionHierarchyRow> after = builder.build(
            Collections.singletonList("https://example.test/a1"), entries, NA);
        Assert.assertEquals(1, SessionHierarchyBuilder.totalSessionCount(after));
        Assert.assertEquals(Integer.valueOf(1),
            SessionHierarchyBuilder.sessionCountByProjectLabel(after).get("projectOne"));
    }
}
