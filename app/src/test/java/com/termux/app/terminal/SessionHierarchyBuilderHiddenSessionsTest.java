package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SessionHierarchyBuilderHiddenSessionsTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    @Test
    public void filterHiddenSessionsReturnsSameRowsWhenNoSessionIsHidden() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, Collections.emptyList(), NA);

        List<SessionHierarchyRow> filtered = SessionHierarchyBuilder.filterHiddenSessions(
            rows, sessionNames, Collections.emptySet());

        Assert.assertEquals(rows, filtered);
    }

    @Test
    public void filterHiddenSessionsExcludesHiddenSessionRowsFromFlatList() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, Collections.emptyList(), NA);
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("beta"));

        List<SessionHierarchyRow> filtered = SessionHierarchyBuilder.filterHiddenSessions(
            rows, sessionNames, hiddenSessionNames);

        Assert.assertEquals(2, SessionHierarchyBuilder.totalSessionCount(filtered));
        List<Integer> visibleSessionIndexes = SessionHierarchyBuilder.visibleSessionIndexes(filtered);
        Assert.assertEquals(Arrays.asList(0, 2), visibleSessionIndexes);
    }

    @Test
    public void filterHiddenSessionsDropsProjectAndStoryHeadersWhoseSessionsAreAllHidden() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a1")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Collections.singletonList("https://example.test/b1")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/b1");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> hiddenSessionNames = new HashSet<>(
            Collections.singletonList("https://example.test/a1"));

        List<SessionHierarchyRow> filtered = SessionHierarchyBuilder.filterHiddenSessions(
            rows, sessionNames, hiddenSessionNames);

        for (SessionHierarchyRow row : filtered) {
            Assert.assertNotEquals("projectOne", row.getLabel());
            Assert.assertNotEquals("storyA", row.getLabel());
        }
        Assert.assertEquals(1, SessionHierarchyBuilder.totalSessionCount(filtered));
    }

    @Test
    public void filterHiddenSessionsKeepsProjectHeaderWhenAtLeastOneSessionRemains() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, entries, NA);
        Set<String> hiddenSessionNames = new HashSet<>(
            Collections.singletonList("https://example.test/a1"));

        List<SessionHierarchyRow> filtered = SessionHierarchyBuilder.filterHiddenSessions(
            rows, sessionNames, hiddenSessionNames);

        boolean projectHeaderRetained = false;
        for (SessionHierarchyRow row : filtered) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER
                    && "projectOne".equals(row.getLabel())) {
                projectHeaderRetained = true;
            }
        }
        Assert.assertTrue(projectHeaderRetained);
        Assert.assertEquals(1, SessionHierarchyBuilder.totalSessionCount(filtered));
    }

    @Test
    public void shownSessionCountEqualsTotalWhenNoSessionIsHidden() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, Collections.emptyList(), NA);

        Assert.assertEquals(3, SessionHierarchyBuilder.shownSessionCount(
            rows, sessionNames, Collections.emptySet()));
    }

    @Test
    public void shownSessionCountExcludesHiddenSessions() {
        List<String> sessionNames = Arrays.asList("alpha", "beta", "gamma");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, Collections.emptyList(), NA);
        Set<String> hiddenSessionNames = new HashSet<>(Arrays.asList("alpha", "gamma"));

        Assert.assertEquals(1, SessionHierarchyBuilder.shownSessionCount(
            rows, sessionNames, hiddenSessionNames));
    }

    @Test
    public void shownSessionCountIsZeroWhenEverySessionIsHidden() {
        List<String> sessionNames = Arrays.asList("alpha", "beta");
        List<SessionHierarchyRow> rows = builder.build(sessionNames, Collections.emptyList(), NA);
        Set<String> hiddenSessionNames = new HashSet<>(Arrays.asList("alpha", "beta"));

        Assert.assertEquals(0, SessionHierarchyBuilder.shownSessionCount(
            rows, sessionNames, hiddenSessionNames));
    }
}
