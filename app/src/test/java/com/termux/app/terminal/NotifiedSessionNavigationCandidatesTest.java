package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotifiedSessionNavigationCandidatesTest {

    @Test
    public void restrictsToNotifiedSessionsInDisplayOrderWhenAnyHaveUnseenBell() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Set<Integer> notified = new LinkedHashSet<>(Arrays.asList(3, 1));

        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(navigable, notified);

        Assert.assertEquals(Arrays.asList(1, 3), candidates);
    }

    @Test
    public void returnsFullNavigableListWhenNoSessionHasUnseenBell() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);

        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(
            navigable, Collections.emptySet());

        Assert.assertEquals(navigable, candidates);
    }

    @Test
    public void ignoresNotifiedSessionsThatAreNotNavigable() {
        List<Integer> navigable = Arrays.asList(0, 2, 4);
        Set<Integer> notified = new LinkedHashSet<>(Arrays.asList(1, 3));

        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(navigable, notified);

        Assert.assertEquals(navigable, candidates);
    }

    @Test
    public void forwardNavigationVisitsOnlyNotifiedSessionsAndWrapsWithinThatSubset() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Set<Integer> notified = new LinkedHashSet<>(Arrays.asList(1, 3));
        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(navigable, notified);

        List<Integer> visited = new ArrayList<>();
        int current = 1;
        visited.add(current);
        for (int step = 0; step < 3; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(ordered, candidates, current, true);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(1, 3, 1, 3), visited);
    }

    @Test
    public void backwardNavigationVisitsOnlyNotifiedSessionsAndWrapsWithinThatSubset() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Set<Integer> notified = new LinkedHashSet<>(Arrays.asList(1, 3));
        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(navigable, notified);

        List<Integer> visited = new ArrayList<>();
        int current = 3;
        visited.add(current);
        for (int step = 0; step < 3; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(ordered, candidates, current, false);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(3, 1, 3, 1), visited);
    }

    @Test
    public void navigationFromNonNotifiedCurrentSessionJumpsToNextNotifiedInDisplayOrder() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Set<Integer> notified = new LinkedHashSet<>(Arrays.asList(1, 4));
        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(navigable, notified);

        Assert.assertEquals(4, VisibleSessionNavigator.nextSessionIndex(ordered, candidates, 2, true));
        Assert.assertEquals(1, VisibleSessionNavigator.nextSessionIndex(ordered, candidates, 2, false));
    }

    @Test
    public void fullNavigationVisitsEverySessionWhenNoSessionHasUnseenBell() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);
        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToNotifiedWhenAny(
            navigable, Collections.emptySet());

        List<Integer> visited = new ArrayList<>();
        int current = 0;
        visited.add(current);
        for (int step = 0; step < 3; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(ordered, candidates, current, true);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(0, 1, 2, 3), visited);
    }
}
