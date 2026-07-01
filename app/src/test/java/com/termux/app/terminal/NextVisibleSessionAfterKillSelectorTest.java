package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NextVisibleSessionAfterKillSelectorTest {

    @Test
    public void selectsTheFollowingVisibleSessionWhenAMiddleSessionIsKilled() {
        List<String> orderedVisibleSessionNames = Arrays.asList("alpha", "beta", "gamma", "delta");
        Assert.assertEquals("gamma",
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "beta"));
    }

    @Test
    public void selectsThePrecedingVisibleSessionWhenTheLastVisibleSessionIsKilled() {
        List<String> orderedVisibleSessionNames = Arrays.asList("alpha", "beta", "gamma", "delta");
        Assert.assertEquals("gamma",
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "delta"));
    }

    @Test
    public void selectsTheFollowingVisibleSessionWhenTheFirstVisibleSessionIsKilled() {
        List<String> orderedVisibleSessionNames = Arrays.asList("alpha", "beta", "gamma", "delta");
        Assert.assertEquals("beta",
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "alpha"));
    }

    @Test
    public void followsBottomSheetDisplayOrderRatherThanAlphabeticalOrNumericOrder() {
        List<String> orderedVisibleSessionNames = Arrays.asList("delta", "beta", "gamma", "alpha");
        Assert.assertEquals("gamma",
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "beta"));
    }

    @Test
    public void returnsNoSelectionWhenTheKilledSessionWasTheOnlyVisibleSession() {
        List<String> orderedVisibleSessionNames = Collections.singletonList("alpha");
        Assert.assertNull(
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "alpha"));
    }

    @Test
    public void returnsNoSelectionWhenNoSessionsAreVisible() {
        Assert.assertNull(
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(Collections.emptyList(), "alpha"));
    }

    @Test
    public void fallsBackToTheFirstRemainingVisibleSessionWhenTheKilledSessionIsNotVisible() {
        List<String> orderedVisibleSessionNames = Arrays.asList("alpha", "beta", "gamma");
        Assert.assertEquals("alpha",
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "hidden"));
    }

    @Test
    public void returnsNoSelectionWhenTheKilledSessionIsNotVisibleAndNoOtherSessionsRemain() {
        List<String> orderedVisibleSessionNames = Collections.singletonList("hidden");
        Assert.assertNull(
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "hidden"));
    }

    @Test
    public void neverSelectsAHiddenOrCollapsedSessionBecauseTheyAreAbsentFromTheVisibleOrder() {
        List<String> orderedVisibleSessionNames = Arrays.asList("visibleBefore", "killed", "visibleAfter");
        String selected =
            NextVisibleSessionAfterKillSelector.selectNextVisibleSessionName(orderedVisibleSessionNames, "killed");
        Assert.assertEquals("visibleAfter", selected);
        Assert.assertNotEquals("hiddenSession", selected);
        Assert.assertNotEquals("collapsedSession", selected);
    }
}
