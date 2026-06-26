package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotifiedSessionNavigationCandidatesTest {

    private static Map<Integer, SessionNewActivityTier> tiers(Object... pairs) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            tiersByIndex.put((Integer) pairs[i], (SessionNewActivityTier) pairs[i + 1]);
        }
        return tiersByIndex;
    }

    @Test
    public void restrictsToRedSessionsWhenAnyRedExists() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            1, SessionNewActivityTier.YELLOW,
            2, SessionNewActivityTier.RED,
            4, SessionNewActivityTier.RED);

        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiersByIndex);

        Assert.assertEquals(Arrays.asList(2, 4), candidates);
    }

    @Test
    public void returnsFullNavigableListWhenNoRedButYellowExists() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            1, SessionNewActivityTier.YELLOW,
            3, SessionNewActivityTier.YELLOW);

        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiersByIndex);

        Assert.assertEquals(navigable, candidates);
    }

    @Test
    public void returnsFullNavigableListWhenNoSessionHasActivity() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);

        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToActiveTier(
            navigable, tiers());

        Assert.assertEquals(navigable, candidates);
    }

    @Test
    public void ignoresActiveSessionsThatAreNotNavigable() {
        List<Integer> navigable = Arrays.asList(0, 2, 4);
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            1, SessionNewActivityTier.RED,
            3, SessionNewActivityTier.RED);

        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiersByIndex);

        Assert.assertEquals(navigable, candidates);
    }

    @Test
    public void redCyclingVisitsOnlyRedSessionsAndWrapsWithinThatSubset() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            0, SessionNewActivityTier.YELLOW,
            1, SessionNewActivityTier.RED,
            3, SessionNewActivityTier.RED);
        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiersByIndex);

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
    public void sequentialCyclingVisitsEverySessionWhenOnlyYellowExists() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            1, SessionNewActivityTier.YELLOW,
            3, SessionNewActivityTier.YELLOW);
        List<Integer> candidates =
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiersByIndex);

        Assert.assertEquals(navigable, candidates);

        List<Integer> visited = new ArrayList<>();
        int current = 0;
        visited.add(current);
        for (int step = 0; step < 5; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(ordered, candidates, current, true);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 0), visited);
    }

    @Test
    public void fullNavigationVisitsEverySessionWhenNoSessionHasActivity() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3);
        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToActiveTier(
            navigable, tiers());

        List<Integer> visited = new ArrayList<>();
        int current = 0;
        visited.add(current);
        for (int step = 0; step < 3; step++) {
            current = VisibleSessionNavigator.nextSessionIndex(ordered, candidates, current, true);
            visited.add(current);
        }

        Assert.assertEquals(Arrays.asList(0, 1, 2, 3), visited);
    }

    @Test
    public void relaxesToSequentialWhenOnlyRedIsCurrentSessionSoArrowMovesToAdjacent() {
        List<Integer> ordered = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        int currentSessionIndex = 2;
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            2, SessionNewActivityTier.RED);

        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToActiveTier(
            navigable, tiersByIndex, currentSessionIndex);

        Assert.assertEquals(navigable, candidates);

        int forwardTarget = VisibleSessionNavigator.nextSessionIndex(
            ordered, candidates, currentSessionIndex, true);
        Assert.assertEquals(3, forwardTarget);

        int backwardTarget = VisibleSessionNavigator.nextSessionIndex(
            ordered, candidates, currentSessionIndex, false);
        Assert.assertEquals(1, backwardTarget);
    }

    @Test
    public void keepsRedCyclingWhenTheSingleRedSessionIsNotTheCurrentSession() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        int currentSessionIndex = 0;
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            3, SessionNewActivityTier.RED);

        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToActiveTier(
            navigable, tiersByIndex, currentSessionIndex);

        Assert.assertEquals(Arrays.asList(3), candidates);
    }

    @Test
    public void keepsRedCyclingWhenMultipleRedSessionsExistIncludingCurrent() {
        List<Integer> navigable = Arrays.asList(0, 1, 2, 3, 4);
        int currentSessionIndex = 1;
        Map<Integer, SessionNewActivityTier> tiersByIndex = tiers(
            1, SessionNewActivityTier.RED,
            4, SessionNewActivityTier.RED);

        List<Integer> candidates = NotifiedSessionNavigationCandidates.restrictToActiveTier(
            navigable, tiersByIndex, currentSessionIndex);

        Assert.assertEquals(Arrays.asList(1, 4), candidates);
    }

    @Test
    public void redRestrictsButYellowAndNoneBothNavigateAllSessions() {
        List<Integer> navigable = Arrays.asList(0, 1, 2);
        Map<Integer, SessionNewActivityTier> withRed = tiers(
            0, SessionNewActivityTier.RED,
            1, SessionNewActivityTier.YELLOW);
        Assert.assertEquals(Arrays.asList(0),
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, withRed));

        Map<Integer, SessionNewActivityTier> yellowOnly = tiers(
            1, SessionNewActivityTier.YELLOW);
        Assert.assertEquals(navigable,
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, yellowOnly));

        Assert.assertEquals(navigable,
            NotifiedSessionNavigationCandidates.restrictToActiveTier(navigable, tiers()));
    }
}
