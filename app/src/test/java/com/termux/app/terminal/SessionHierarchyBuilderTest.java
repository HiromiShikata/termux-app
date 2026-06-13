package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionHierarchyBuilderTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String OTHER = "Other";

    @Test
    public void buildsProjectThenStoryThenSessionRowsForASingleStory() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList("https://example.test/a"), entries, OTHER);

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
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, OTHER);

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
            Arrays.asList("https://example.test/a", "https://example.test/b"), entries, OTHER);

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
            Arrays.asList("https://example.test/a1", "https://example.test/a2"), entries, OTHER);

        Assert.assertEquals(4, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
        assertSession(rows.get(3), 1);
    }

    @Test
    public void placesUnmatchedSessionsUnderAFinalOtherGroupWithoutAStoryHeader() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"), entries, OTHER);

        Assert.assertEquals(5, rows.size());
        assertProjectHeader(rows.get(0), "projectOne");
        assertStoryHeader(rows.get(1), "storyA");
        assertSession(rows.get(2), 0);
        assertProjectHeader(rows.get(3), OTHER);
        assertSession(rows.get(4), 1);
    }

    @Test
    public void fallsBackToAFlatSessionListWhenEntriesAreEmpty() {
        List<SessionHierarchyRow> rows = builder.build(
            Arrays.asList("https://example.test/a", "manual-session"),
            Collections.emptyList(), OTHER);

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
            Collections.emptyList(), entries, OTHER);

        Assert.assertTrue(rows.isEmpty());
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
