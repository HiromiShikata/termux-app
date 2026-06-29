package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class TermuxSessionsListViewControllerAllSessionIndicatorsTest {

    @Test
    public void offScreenSessionGetsAnIndicatorWhenAllIndexesAreComputed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("on-screen", 1_000L);
        store.recordOutputActivity("off-screen", 1_000L);

        Map<Integer, SessionNewActivityIndicator> indicators =
            TermuxSessionsListViewController.sessionActivityIndicatorsByIndex(
                store, Arrays.asList(0, 1), Arrays.asList("on-screen", "off-screen"),
                Collections.emptySet(), 2_000L);

        Assert.assertTrue(indicators.containsKey(0));
        Assert.assertTrue(indicators.containsKey(1));
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicators.get(1).getTier());
    }

    @Test
    public void offScreenSessionIsMissedWhenOnlyVisibleIndexesAreComputed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("on-screen", 1_000L);
        store.recordOutputActivity("off-screen", 1_000L);

        Map<Integer, SessionNewActivityIndicator> visibleOnlyIndicators =
            TermuxSessionsListViewController.sessionActivityIndicatorsByIndex(
                store, Collections.singletonList(0), Arrays.asList("on-screen", "off-screen"),
                Collections.emptySet(), 2_000L);

        Assert.assertTrue(visibleOnlyIndicators.containsKey(0));
        Assert.assertFalse(visibleOnlyIndicators.containsKey(1));
    }
}
