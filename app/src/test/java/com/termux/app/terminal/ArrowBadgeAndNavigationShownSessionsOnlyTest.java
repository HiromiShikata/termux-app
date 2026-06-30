package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArrowBadgeAndNavigationShownSessionsOnlyTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    private static List<Integer> orderedShownSessionIndexes(SessionHierarchyBuilder builder,
                                                            List<String> sessionNames,
                                                            List<SessionDefinitionEntry> entries,
                                                            Set<String> hiddenSessionNames,
                                                            Set<String> collapsedProjectKeys) {
        List<SessionHierarchyRow> allRows = builder.build(sessionNames, entries, NA);
        List<SessionHierarchyRow> renderedRows =
            SessionHierarchyBuilder.filterHiddenSessions(allRows, sessionNames, hiddenSessionNames);
        List<SessionHierarchyRow> countedRows =
            SessionHierarchyBuilder.filterCollapsedProjectSessions(renderedRows, collapsedProjectKeys);
        return SessionHierarchyBuilder.visibleSessionIndexes(countedRows);
    }

    private static Map<Integer, SessionNewActivityTier> redTierFor(List<String> sessionNames,
                                                                   Set<String> pendingCallSessionNames) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            if (pendingCallSessionNames.contains(sessionNames.get(sessionIndex))) {
                tiersByIndex.put(sessionIndex, SessionNewActivityTier.RED);
            }
        }
        return tiersByIndex;
    }

    @Test
    public void hiddenSessionPendingCallIsNotCountedInTheArrowBadgeWhenTheHideToggleIsOn() {
        List<String> sessionNames = Arrays.asList("shownAbove", "current", "hiddenBelow");
        Set<String> hidden = new HashSet<>(Collections.singletonList("hiddenBelow"));
        Set<String> pendingCalls = new HashSet<>(Arrays.asList("shownAbove", "hiddenBelow"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, Collections.emptyList(), hidden, Collections.emptySet());
        int currentSessionIndex = 1;
        SessionActivityDirection badge = SessionActivityDirection.compute(
            ordered, currentSessionIndex, redTierFor(sessionNames, pendingCalls));

        Assert.assertEquals(1, badge.getActiveAboveCount());
        Assert.assertEquals(0, badge.getActiveBelowCount());
    }

    @Test
    public void arrowDoesNotNavigateToAHiddenSessionWhenTheHideToggleIsOn() {
        List<String> sessionNames = Arrays.asList("shown", "hidden");
        Set<String> hidden = new HashSet<>(Collections.singletonList("hidden"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, Collections.emptyList(), hidden, Collections.emptySet());

        Assert.assertEquals(Collections.singletonList(0), ordered);
        Assert.assertEquals(0,
            VisibleSessionNavigator.nextSessionIndex(ordered, ordered, 0, true));
    }

    @Test
    public void clearingEveryShownPendingCallClearsTheBadgeEvenWhenAHiddenSessionStillHasAPendingCall() {
        List<String> sessionNames = Arrays.asList("shown", "current", "hidden");
        Set<String> hidden = new HashSet<>(Collections.singletonList("hidden"));
        Set<String> pendingCallsAfterClearingShown =
            new HashSet<>(Collections.singletonList("hidden"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, Collections.emptyList(), hidden, Collections.emptySet());
        SessionActivityDirection badge = SessionActivityDirection.compute(
            ordered, 1, redTierFor(sessionNames, pendingCallsAfterClearingShown));

        Assert.assertEquals(SessionNewActivityTier.NONE, badge.getTier());
        Assert.assertEquals(0, badge.getActiveAboveCount());
        Assert.assertEquals(0, badge.getActiveBelowCount());
    }

    @Test
    public void collapsedProjectPendingCallIsNotCountedInTheArrowBadge() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Collections.singletonList("https://example.test/collapsed")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Arrays.asList("https://example.test/current", "https://example.test/shownBelow")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/collapsed", "https://example.test/current",
            "https://example.test/shownBelow");
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));
        Set<String> pendingCalls = new HashSet<>(Arrays.asList(
            "https://example.test/collapsed", "https://example.test/shownBelow"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, entries, Collections.emptySet(), collapsed);
        int currentSessionIndex = 1;
        SessionActivityDirection badge = SessionActivityDirection.compute(
            ordered, currentSessionIndex, redTierFor(sessionNames, pendingCalls));

        Assert.assertEquals(0, badge.getActiveAboveCount());
        Assert.assertEquals(1, badge.getActiveBelowCount());
    }

    @Test
    public void arrowDoesNotNavigateToACollapsedProjectSession() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Collections.singletonList("https://example.test/collapsed")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Arrays.asList("https://example.test/first", "https://example.test/second")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/collapsed", "https://example.test/first",
            "https://example.test/second");
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, entries, Collections.emptySet(), collapsed);

        Assert.assertFalse("collapsed-project session must not be a navigation target",
            ordered.contains(0));
        Assert.assertEquals(2,
            VisibleSessionNavigator.nextSessionIndex(ordered, ordered, 1, true));
        Assert.assertEquals(1,
            VisibleSessionNavigator.nextSessionIndex(ordered, ordered, 2, true));
    }

    @Test
    public void hiddenAndCollapsedExclusionsApplyTogetherToBothTheBadgeAndTheNavigationSet() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("collapsedProject", "storyA",
                Collections.singletonList("https://example.test/collapsed")),
            new SessionDefinitionEntry("expandedProject", "storyB",
                Arrays.asList("https://example.test/current", "https://example.test/hidden",
                    "https://example.test/shown")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/collapsed", "https://example.test/current",
            "https://example.test/hidden", "https://example.test/shown");
        Set<String> hidden = new HashSet<>(Collections.singletonList("https://example.test/hidden"));
        Set<String> collapsed = new LinkedHashSet<>(Collections.singletonList("collapsedProject"));
        Set<String> pendingCalls = new HashSet<>(Arrays.asList(
            "https://example.test/collapsed", "https://example.test/hidden",
            "https://example.test/shown"));

        List<Integer> ordered = orderedShownSessionIndexes(
            builder, sessionNames, entries, hidden, collapsed);

        Assert.assertEquals(Arrays.asList(1, 3), ordered);
        SessionActivityDirection badge = SessionActivityDirection.compute(
            ordered, 1, redTierFor(sessionNames, pendingCalls));
        Assert.assertEquals(0, badge.getActiveAboveCount());
        Assert.assertEquals(1, badge.getActiveBelowCount());
        Assert.assertEquals(3,
            VisibleSessionNavigator.nextSessionIndex(ordered, ordered, 1, true));
        Assert.assertEquals(1,
            VisibleSessionNavigator.nextSessionIndex(ordered, ordered, 3, true));
    }
}
