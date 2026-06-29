package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionHierarchyBuilderVisibleFractionTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    private List<SessionHierarchyRow> renderedRows(List<String> sessionNames,
                                                   List<SessionDefinitionEntry> entries,
                                                   Set<String> hiddenSessionNames,
                                                   boolean hidingHiddenSessions) {
        List<SessionHierarchyRow> allRows = builder.build(sessionNames, entries, NA);
        Set<String> effectiveHiddenSessionNames =
            hidingHiddenSessions ? hiddenSessionNames : Collections.emptySet();
        return SessionHierarchyBuilder.filterHiddenSessions(allRows, sessionNames, effectiveHiddenSessionNames);
    }

    @Test
    public void showingAllSessionsDenominatorIsTheFullSessionCountAndNumeratorCountsEveryPendingCall() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2",
                    "https://example.test/a3")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/a3");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("https://example.test/a2"));
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a1", "https://example.test/a2"));

        List<SessionHierarchyRow> renderedRows =
            renderedRows(sessionNames, entries, hiddenSessionNames, false);

        Assert.assertEquals("denominator must be every session when showing all",
            3, SessionHierarchyBuilder.totalSessionCount(renderedRows));
        Assert.assertEquals("numerator must count every pending call when showing all, including the hidden one",
            2, SessionHierarchyBuilder.pendingCallSessionCount(renderedRows, sessionNames, pendingCallSessionNames));
    }

    @Test
    public void hidingHiddenSessionsDenominatorExcludesHiddenRowsAndNumeratorExcludesHiddenPendingCalls() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2",
                    "https://example.test/a3")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2", "https://example.test/a3");
        Set<String> hiddenSessionNames = new HashSet<>(Collections.singletonList("https://example.test/a2"));
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a1", "https://example.test/a2"));

        List<SessionHierarchyRow> renderedRows =
            renderedRows(sessionNames, entries, hiddenSessionNames, true);

        Assert.assertEquals("denominator must drop the hidden session when filtering",
            2, SessionHierarchyBuilder.totalSessionCount(renderedRows));
        Assert.assertEquals("numerator must not count the hidden session's pending call when filtering",
            1, SessionHierarchyBuilder.pendingCallSessionCount(renderedRows, sessionNames, pendingCallSessionNames));
    }

    @Test
    public void perProjectFractionCountsOnlyAmongThatProjectsVisibleSessions() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Arrays.asList("https://example.test/b1", "https://example.test/b2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2",
            "https://example.test/b1", "https://example.test/b2");
        Set<String> hiddenSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a2", "https://example.test/b1"));
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a2", "https://example.test/b2"));

        List<SessionHierarchyRow> renderedRows =
            renderedRows(sessionNames, entries, hiddenSessionNames, true);

        Map<String, Integer> visibleCountByProject =
            SessionHierarchyBuilder.sessionCountByProjectLabel(renderedRows);
        Map<String, Integer> pendingByProject = SessionHierarchyBuilder.pendingCallSessionCountByProjectLabel(
            renderedRows, sessionNames, pendingCallSessionNames);

        Assert.assertEquals("projectOne visible denominator excludes its hidden session",
            Integer.valueOf(1), visibleCountByProject.get("projectOne"));
        Assert.assertEquals("projectOne numerator drops the hidden pending call so it reports zero",
            Integer.valueOf(0), pendingByProject.get("projectOne"));
        Assert.assertEquals("projectTwo visible denominator excludes its hidden session",
            Integer.valueOf(1), visibleCountByProject.get("projectTwo"));
        Assert.assertEquals("projectTwo numerator keeps its visible pending call",
            Integer.valueOf(1), pendingByProject.get("projectTwo"));
    }

    @Test
    public void perProjectFractionShowingAllIncludesHiddenSessionsInBothNumeratorAndDenominator() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Arrays.asList("https://example.test/a1", "https://example.test/a2")),
            new SessionDefinitionEntry("projectTwo", "storyB",
                Arrays.asList("https://example.test/b1", "https://example.test/b2")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a1", "https://example.test/a2",
            "https://example.test/b1", "https://example.test/b2");
        Set<String> hiddenSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a2", "https://example.test/b1"));
        Set<String> pendingCallSessionNames = new HashSet<>(Arrays.asList(
            "https://example.test/a2", "https://example.test/b2"));

        List<SessionHierarchyRow> renderedRows =
            renderedRows(sessionNames, entries, hiddenSessionNames, false);

        Map<String, Integer> visibleCountByProject =
            SessionHierarchyBuilder.sessionCountByProjectLabel(renderedRows);
        Map<String, Integer> pendingByProject = SessionHierarchyBuilder.pendingCallSessionCountByProjectLabel(
            renderedRows, sessionNames, pendingCallSessionNames);

        Assert.assertEquals(Integer.valueOf(2), visibleCountByProject.get("projectOne"));
        Assert.assertEquals(Integer.valueOf(1), pendingByProject.get("projectOne"));
        Assert.assertEquals(Integer.valueOf(2), visibleCountByProject.get("projectTwo"));
        Assert.assertEquals(Integer.valueOf(1), pendingByProject.get("projectTwo"));
    }
}
