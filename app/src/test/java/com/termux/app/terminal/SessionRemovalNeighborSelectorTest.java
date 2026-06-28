package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionRemovalNeighborSelectorTest {

    @Test
    public void selectsTheFollowingSessionWhenAMiddleSessionIsRemoved() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(0, 1, 2, 3);
        Assert.assertEquals(2,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 1));
    }

    @Test
    public void selectsThePrecedingSessionWhenTheLastSessionIsRemoved() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(0, 1, 2, 3);
        Assert.assertEquals(2,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 3));
    }

    @Test
    public void selectsTheFollowingSessionWhenTheFirstSessionIsRemoved() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(0, 1, 2, 3);
        Assert.assertEquals(1,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 0));
    }

    @Test
    public void doesNotJumpToTheFirstSessionWhenAMiddleSessionIsRemoved() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(10, 11, 12, 13);
        int neighbor = SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 12);
        Assert.assertEquals(13, neighbor);
        Assert.assertNotEquals(orderedVisibleSessionIndexes.get(0).intValue(), neighbor);
    }

    @Test
    public void honorsVisibleDisplayOrderRatherThanNumericSessionIndex() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(3, 1, 2, 0);
        Assert.assertEquals(2,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 1));
    }

    @Test
    public void returnsNoNeighborWhenTheRemovedSessionWasTheOnlyVisibleSession() {
        List<Integer> orderedVisibleSessionIndexes = Collections.singletonList(0);
        Assert.assertEquals(SessionRemovalNeighborSelector.NO_NEIGHBOR,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 0));
    }

    @Test
    public void fallsBackToFirstRemainingSessionWhenRemovedSessionIsNotInVisibleOrder() {
        List<Integer> orderedVisibleSessionIndexes = Arrays.asList(0, 1, 2);
        Assert.assertEquals(0,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(orderedVisibleSessionIndexes, 9));
    }

    @Test
    public void returnsNoNeighborWhenVisibleOrderIsEmpty() {
        Assert.assertEquals(SessionRemovalNeighborSelector.NO_NEIGHBOR,
            SessionRemovalNeighborSelector.selectNeighborSessionIndex(Collections.emptyList(), 0));
    }
}
