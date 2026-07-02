package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class NextVisibleSessionAfterKillSelectorTest {

    @Test
    public void selectsTheFollowingVisiblePositionWhenAMiddleSessionIsKilled() {
        Assert.assertEquals(2,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(4, 1));
    }

    @Test
    public void selectsThePrecedingVisiblePositionWhenTheLastVisibleSessionIsKilled() {
        Assert.assertEquals(2,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(4, 3));
    }

    @Test
    public void selectsTheFollowingVisiblePositionWhenTheFirstVisibleSessionIsKilled() {
        Assert.assertEquals(1,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(4, 0));
    }

    @Test
    public void returnsNoSelectionWhenTheKilledSessionWasTheOnlyVisibleSession() {
        Assert.assertEquals(NextVisibleSessionAfterKillSelector.NO_SELECTION,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(1, 0));
    }

    @Test
    public void returnsNoSelectionWhenNoSessionsAreVisible() {
        Assert.assertEquals(NextVisibleSessionAfterKillSelector.NO_SELECTION,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
                0, NextVisibleSessionAfterKillSelector.NO_SELECTION));
    }

    @Test
    public void fallsBackToTheFirstRemainingVisiblePositionWhenTheKilledSessionIsNotVisible() {
        Assert.assertEquals(0,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
                3, NextVisibleSessionAfterKillSelector.NO_SELECTION));
    }

    @Test
    public void returnsNoSelectionWhenTheKilledSessionIsNotVisibleAndNoOtherSessionsRemain() {
        Assert.assertEquals(NextVisibleSessionAfterKillSelector.NO_SELECTION,
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
                0, NextVisibleSessionAfterKillSelector.NO_SELECTION));
    }

    @Test
    public void resolvesTheKilledPositionFromItsUniqueServiceIndexEvenWhenNamesAreDuplicated() {
        List<Integer> orderedServiceIndexesInDisplayOrder = Arrays.asList(3, 1, 2, 0);
        String[] sessionNamesByServiceIndex = {"work", "work", "gamma", "work"};

        int killedServiceIndex = 1;
        int killedVisiblePosition = orderedServiceIndexesInDisplayOrder.indexOf(killedServiceIndex);
        Assert.assertEquals(1, killedVisiblePosition);

        int neighborVisiblePosition = NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
            orderedServiceIndexesInDisplayOrder.size(), killedVisiblePosition);
        int neighborServiceIndex = orderedServiceIndexesInDisplayOrder.get(neighborVisiblePosition);

        Assert.assertEquals(2, neighborServiceIndex);
        Assert.assertEquals("gamma", sessionNamesByServiceIndex[neighborServiceIndex]);
    }

    @Test
    public void selectsTheBelowNeighborInVisibleOrderRatherThanServiceCreationOrder() {
        List<Integer> orderedServiceIndexesInDisplayOrder = Arrays.asList(3, 0, 2, 1);

        int killedServiceIndex = 0;
        int killedVisiblePosition = orderedServiceIndexesInDisplayOrder.indexOf(killedServiceIndex);
        int neighborVisiblePosition = NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
            orderedServiceIndexesInDisplayOrder.size(), killedVisiblePosition);
        int neighborServiceIndex = orderedServiceIndexesInDisplayOrder.get(neighborVisiblePosition);

        Assert.assertEquals(2, neighborServiceIndex);
    }

    @Test
    public void selectsTheAboveNeighborWhenTheLastSessionInVisibleOrderIsKilledRegardlessOfServiceOrder() {
        List<Integer> orderedServiceIndexesInDisplayOrder = Arrays.asList(3, 0, 2, 1);

        int killedServiceIndex = 1;
        int killedVisiblePosition = orderedServiceIndexesInDisplayOrder.indexOf(killedServiceIndex);
        int neighborVisiblePosition = NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
            orderedServiceIndexesInDisplayOrder.size(), killedVisiblePosition);
        int neighborServiceIndex = orderedServiceIndexesInDisplayOrder.get(neighborVisiblePosition);

        Assert.assertEquals(2, neighborServiceIndex);
    }
}
