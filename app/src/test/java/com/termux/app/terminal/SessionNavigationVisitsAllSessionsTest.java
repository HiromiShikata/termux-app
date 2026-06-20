package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionNavigationVisitsAllSessionsTest {

    @Test
    public void forwardNavigationVisitsEverySessionTopToBottomInOrder() {
        List<Integer> orderedSessionIndexes = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigableSessionIndexes = Arrays.asList(0, 1, 2, 3, 4);

        List<Integer> visited = new ArrayList<>();
        int current = 0;
        visited.add(current);
        for (int step = 0; step < orderedSessionIndexes.size() - 1; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(
                orderedSessionIndexes, navigableSessionIndexes, current, true);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4), visited);
    }

    @Test
    public void backwardNavigationVisitsEverySessionBottomToTopInOrder() {
        List<Integer> orderedSessionIndexes = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigableSessionIndexes = Arrays.asList(0, 1, 2, 3, 4);

        List<Integer> visited = new ArrayList<>();
        int current = 4;
        visited.add(current);
        for (int step = 0; step < orderedSessionIndexes.size() - 1; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(
                orderedSessionIndexes, navigableSessionIndexes, current, false);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(4, 3, 2, 1, 0), visited);
    }

    @Test
    public void navigableSetMatchesOrderedSetWhenNoSessionIsDisabled() {
        List<Integer> visibleSessionIndexes = Arrays.asList(0, 1, 2, 3);

        List<Integer> navigable = TermuxSessionsListViewController.navigableSessionIndexes(
            visibleSessionIndexes, Arrays.asList("a", "b", "c", "d"), java.util.Collections.emptySet());

        Assert.assertEquals(visibleSessionIndexes, navigable);
    }

    @Test
    public void navigationIsNotRestrictedToASubsetWhenSomeSessionsHaveNewActivity() {
        List<Integer> orderedSessionIndexes = Arrays.asList(0, 1, 2);
        List<Integer> navigableSessionIndexes = Arrays.asList(0, 1, 2);

        Assert.assertEquals(1,
            VisibleSessionNavigator.nextSessionIndex(orderedSessionIndexes, navigableSessionIndexes, 0, true));
        Assert.assertEquals(2,
            VisibleSessionNavigator.nextSessionIndex(orderedSessionIndexes, navigableSessionIndexes, 1, true));
        Assert.assertEquals(0,
            VisibleSessionNavigator.nextSessionIndex(orderedSessionIndexes, navigableSessionIndexes, 2, true));
    }
}
