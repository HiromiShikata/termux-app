package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CallingSessionSplitConsistencyTest {

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

    private static void assertBellEqualsUpPlusDownPlusCurrent(List<Integer> ordered,
                                                              List<String> names,
                                                              Set<String> callingSessionNames,
                                                              int currentSessionIndex) {
        CallingSessionSplit split = CallingSessionNavigator.split(
            ordered, names, callingSessionNames, currentSessionIndex);
        int bellCount = CallingSessionNavigator.callingSessionCount(ordered, names, callingSessionNames);
        SessionActivityDirection direction = SessionActivityDirection.compute(
            ordered, currentSessionIndex, names, callingSessionNames);

        Assert.assertEquals(split.getAboveCount(), direction.getActiveAboveCount());
        Assert.assertEquals(split.getBelowCount(), direction.getActiveBelowCount());
        Assert.assertEquals(
            direction.getActiveAboveCount()
                + direction.getActiveBelowCount()
                + (split.isCurrentCalling() ? 1 : 0),
            bellCount);
    }

    @Test
    public void bellEqualsUpPlusDownWithCallingSessionsAboveAndBelowTheCurrent() {
        List<String> names = Arrays.asList("above1", "above2", "current", "below1");
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        Set<String> calling = new HashSet<>(Arrays.asList("above1", "above2", "below1"));

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, 2);

        CallingSessionSplit split = CallingSessionNavigator.split(ordered, names, calling, 2);
        Assert.assertEquals(2, split.getAboveCount());
        Assert.assertEquals(1, split.getBelowCount());
        Assert.assertFalse(split.isCurrentCalling());
        Assert.assertEquals(3, split.getTotalCount());
    }

    @Test
    public void currentCallingSessionCountsInTheBellTotalButNotInEitherArrow() {
        List<String> names = Arrays.asList("above", "current", "below");
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        Set<String> calling = new HashSet<>(Arrays.asList("above", "current", "below"));

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, 1);

        CallingSessionSplit split = CallingSessionNavigator.split(ordered, names, calling, 1);
        Assert.assertEquals(1, split.getAboveCount());
        Assert.assertEquals(1, split.getBelowCount());
        Assert.assertTrue(split.isCurrentCalling());
        Assert.assertEquals(3, split.getTotalCount());
    }

    @Test
    public void disabledButDisplayedCallingSessionIsCountedByBothTheBellAndTheArrows() {
        List<String> names = Arrays.asList("shownAbove", "current", "disabledDisplayedBelow");
        List<Integer> ordered = orderedShownSessionIndexes(
            builder, names, Collections.emptyList(), Collections.emptySet(), Collections.emptySet());
        Assert.assertEquals(Arrays.asList(0, 1, 2), ordered);
        Set<String> calling = new HashSet<>(Arrays.asList("shownAbove", "disabledDisplayedBelow"));

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, 1);

        SessionActivityDirection direction = SessionActivityDirection.compute(ordered, 1, names, calling);
        Assert.assertEquals(1, direction.getActiveAboveCount());
        Assert.assertEquals(1, direction.getActiveBelowCount());
        Assert.assertEquals(2,
            CallingSessionNavigator.callingSessionCount(ordered, names, calling));
    }

    @Test
    public void everyCountedCallingSessionIsReachableByTheBellAndTheArrows() {
        List<String> names = Arrays.asList("callAbove", "current", "disabledCallBelow", "callBelow");
        List<Integer> ordered = orderedShownSessionIndexes(
            builder, names, Collections.emptyList(), Collections.emptySet(), Collections.emptySet());
        Assert.assertEquals(Arrays.asList(0, 1, 2, 3), ordered);
        Set<String> calling = new LinkedHashSet<>(
            Arrays.asList("callAbove", "disabledCallBelow", "callBelow"));
        Set<String> disabled = new HashSet<>(Collections.singletonList("disabledCallBelow"));
        int currentSessionIndex = 1;

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, currentSessionIndex);

        int topmost = CallingSessionNavigator.topmostCallingSessionIndex(ordered, names, calling);
        Assert.assertEquals(0, topmost);

        List<Integer> navigable =
            TermuxSessionsListViewController.navigableSessionIndexes(ordered, names, disabled);
        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToCallingSessions(
            ordered, navigable, names, calling, currentSessionIndex);
        Assert.assertEquals(Arrays.asList(0, 2, 3), candidates);
        for (int sessionIndex : ordered) {
            String sessionName = names.get(sessionIndex);
            if (calling.contains(sessionName)) {
                Assert.assertTrue(
                    "counted calling session must be a navigation candidate",
                    candidates.contains(sessionIndex));
            }
        }
    }

    @Test
    public void noCallingSessionsProducesZeroEverywhere() {
        List<String> names = Arrays.asList("a", "b", "c");
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        Set<String> calling = Collections.emptySet();

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, 1);

        CallingSessionSplit split = CallingSessionNavigator.split(ordered, names, calling, 1);
        Assert.assertEquals(0, split.getTotalCount());
        SessionActivityDirection direction = SessionActivityDirection.compute(ordered, 1, names, calling);
        Assert.assertEquals(SessionNewActivityTier.NONE, direction.getTier());
    }

    @Test
    public void absentCurrentSessionCountsEveryCallingSessionAsAboveAndInTheBellTotal() {
        List<String> names = Arrays.asList("a", "b", "c");
        List<Integer> ordered = Arrays.asList(0, 1, 2);
        Set<String> calling = new HashSet<>(Arrays.asList("a", "c"));

        assertBellEqualsUpPlusDownPlusCurrent(ordered, names, calling, -1);

        CallingSessionSplit split = CallingSessionNavigator.split(ordered, names, calling, -1);
        Assert.assertEquals(2, split.getAboveCount());
        Assert.assertEquals(0, split.getBelowCount());
        Assert.assertFalse(split.isCurrentCalling());
        Assert.assertEquals(2, split.getTotalCount());
    }
}
