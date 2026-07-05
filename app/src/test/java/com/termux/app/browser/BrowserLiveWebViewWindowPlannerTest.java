package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserLiveWebViewWindowPlannerTest {

    @Test
    public void displayedOwnerIsAlwaysKeptLiveEvenWhenNotInMostRecentList() {
        String displayed = "displayed";
        List<String> live = Arrays.asList("a", "b", "c", "d");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(displayed, live, 4);

        Assert.assertTrue(plan.getOwnersToKeepLive().contains(displayed));
        Assert.assertEquals(4, plan.getOwnersToKeepLive().size());
    }

    @Test
    public void ownersBeyondWindowAreReleasedMostRecentlyUsedWins() {
        String displayed = "current";
        List<String> liveMostRecentFirst = Arrays.asList("current", "recent1", "recent2", "old1", "old2");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(displayed, liveMostRecentFirst, 3);

        Assert.assertEquals(Arrays.asList("current", "recent1", "recent2"), plan.getOwnersToKeepLive());
        Assert.assertEquals(Arrays.asList("old1", "old2"), plan.getOwnersToRelease());
    }

    @Test
    public void nothingIsReleasedWhenLiveCountFitsWithinWindow() {
        String displayed = "current";
        List<String> live = Arrays.asList("current", "other");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(displayed, live, 4);

        Assert.assertTrue(plan.getOwnersToRelease().isEmpty());
        Assert.assertEquals(Arrays.asList("current", "other"), plan.getOwnersToKeepLive());
    }

    @Test
    public void displayedOwnerIsNeverReleasedEvenWithWindowSizeOne() {
        String displayed = "current";
        List<String> liveMostRecentFirst = Arrays.asList("current", "recent", "old");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(displayed, liveMostRecentFirst, 1);

        Assert.assertEquals(Collections.singletonList("current"), plan.getOwnersToKeepLive());
        Assert.assertEquals(Arrays.asList("recent", "old"), plan.getOwnersToRelease());
        Assert.assertFalse(plan.getOwnersToRelease().contains(displayed));
    }

    @Test
    public void windowSizeIsAtLeastOneWhenGivenZeroOrNegative() {
        String displayed = "current";
        List<String> live = Arrays.asList("current", "other");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(displayed, live, 0);

        Assert.assertEquals(Collections.singletonList("current"), plan.getOwnersToKeepLive());
        Assert.assertEquals(Collections.singletonList("other"), plan.getOwnersToRelease());
    }

    @Test
    public void nullDisplayedOwnerKeepsMostRecentWithinWindow() {
        List<String> liveMostRecentFirst = Arrays.asList("recent1", "recent2", "old");

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(null, liveMostRecentFirst, 2);

        Assert.assertEquals(Arrays.asList("recent1", "recent2"), plan.getOwnersToKeepLive());
        Assert.assertEquals(Collections.singletonList("old"), plan.getOwnersToRelease());
    }

    @Test
    public void emptyLiveListWithNoDisplayedOwnerReleasesNothing() {
        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve(null, new ArrayList<>(), 4);

        Assert.assertTrue(plan.getOwnersToKeepLive().isEmpty());
        Assert.assertTrue(plan.getOwnersToRelease().isEmpty());
    }

    @Test
    public void manyTabsAreBoundedToWindowSizeSoLiveCountCannotGrowUnbounded() {
        List<String> live = new ArrayList<>();
        for (int i = 0; i < 55; i++) live.add("tab" + i);

        BrowserLiveWebViewWindowPlanner<String> plan =
            BrowserLiveWebViewWindowPlanner.resolve("tab0", live, 4);

        Assert.assertEquals(4, plan.getOwnersToKeepLive().size());
        Assert.assertEquals(51, plan.getOwnersToRelease().size());
    }
}
