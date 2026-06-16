package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BellMarkedSessionNavigationFilterTest {

    private static Set<Integer> markedIndexes(Integer... sessionIndexes) {
        return new LinkedHashSet<>(Arrays.asList(sessionIndexes));
    }

    @Test
    public void returnsNavigableUnchangedWhenNoSessionIsMarked() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);

        List<Integer> result = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            navigable, Collections.emptySet());

        Assert.assertEquals(Arrays.asList(0, 1, 2, 3), result);
    }

    @Test
    public void restrictsToMarkedNavigableSessionsWhenSomeAreMarked() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);

        List<Integer> result = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            navigable, markedIndexes(1, 3));

        Assert.assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void preservesNavigableOrderWhenRestrictingToMarkedSessions() {
        List<Integer> navigable = Arrays.asList(2, 3, 0, 1);

        List<Integer> result = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            navigable, markedIndexes(0, 2));

        Assert.assertEquals(Arrays.asList(2, 0), result);
    }

    @Test
    public void ignoresMarkedSessionsThatAreNotNavigable() {
        List<Integer> navigable = Arrays.asList(0, 1, 2);

        List<Integer> result = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            navigable, markedIndexes(5));

        Assert.assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void bellOnDisabledSessionDoesNotEnableBellOnlyMode() {
        List<Integer> disabledFilteredNavigable = Arrays.asList(0, 1, 3);

        List<Integer> result = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            disabledFilteredNavigable, markedIndexes(2));

        Assert.assertEquals(Arrays.asList(0, 1, 3), result);
    }

    @Test
    public void cyclesForwardOnlyThroughMarkedSessionsWhenSomeAreMarked() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            Arrays.asList(0, 1, 2, 3), markedIndexes(1, 3));

        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
    }

    @Test
    public void cyclesBackwardOnlyThroughMarkedSessionsWhenSomeAreMarked() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            Arrays.asList(0, 1, 2, 3), markedIndexes(1, 3));

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, false));
        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, false));
    }

    @Test
    public void markedNavigationSkipsUnmarkedSessionEvenWhenItIsTheCurrentSession() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            Arrays.asList(0, 1, 2, 3), markedIndexes(1, 3));

        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, false));
    }

    @Test
    public void cyclesFullNavigableOrderWhenNoSessionIsMarked() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            Arrays.asList(0, 1, 2, 3), Collections.emptySet());

        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 0, true));
        Assert.assertEquals(2, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 2, true));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
    }

    @Test
    public void disabledSessionIsNeverVisitedWhenNoSessionIsMarked() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> disabledFilteredNavigable = Arrays.asList(0, 1, 3);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            disabledFilteredNavigable, Collections.emptySet());

        Assert.assertEquals(3, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
        Assert.assertEquals(0, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 3, true));
        Assert.assertFalse(navigable.contains(2));
    }

    @Test
    public void disabledSessionIsNeverVisitedWhenSomeSessionsAreMarked() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> disabledFilteredNavigable = Arrays.asList(0, 1, 3, 4);
        List<Integer> navigable = BellMarkedSessionNavigationFilter.bellRestrictedNavigableIndexes(
            disabledFilteredNavigable, markedIndexes(1, 4));

        Assert.assertEquals(Arrays.asList(1, 4), navigable);
        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 1, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, navigable, 4, true));
        Assert.assertFalse(navigable.contains(2));
    }
}
