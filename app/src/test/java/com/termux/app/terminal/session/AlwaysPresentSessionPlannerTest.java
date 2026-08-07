package com.termux.app.terminal.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysPresentSessionPlannerTest {

    private final AlwaysPresentSessionPlanner planner = new AlwaysPresentSessionPlanner();

    @Test
    public void planMissingSessionNamesReturnsAllConfiguredNamesWhenNoSessionsAreLive() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Collections.emptyList(), Collections.emptySet(), Collections.emptySet());

        Assert.assertEquals(Arrays.asList("alpha", "beta"), missing);
    }

    @Test
    public void planMissingSessionNamesSkipsNamesAlreadyLive() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Collections.singletonList("alpha"), Collections.emptySet(), Collections.emptySet());

        Assert.assertEquals(Collections.singletonList("beta"), missing);
    }

    @Test
    public void planMissingSessionNamesReturnsEmptyWhenAllConfiguredNamesAreLive() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Arrays.asList("alpha", "beta"), Collections.emptySet(), Collections.emptySet());

        Assert.assertTrue(missing.isEmpty());
    }

    @Test
    public void planMissingSessionNamesReturnsEmptyWhenNoNamesAreConfigured() {
        List<String> missing = planner.planMissingSessionNames(
            Collections.emptyList(), Collections.singletonList("alpha"), Collections.emptySet(), Collections.emptySet());

        Assert.assertTrue(missing.isEmpty());
    }

    @Test
    public void planMissingSessionNamesDeduplicatesAndTrimsConfiguredNames() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList(" alpha ", "alpha", "beta"), Collections.emptyList(), Collections.emptySet(), Collections.emptySet());

        Assert.assertEquals(Arrays.asList("alpha", "beta"), missing);
    }

    @Test
    public void planMissingSessionNamesIgnoresNullAndBlankConfiguredNames() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList(null, "   ", "alpha"), Collections.emptyList(), Collections.emptySet(), Collections.emptySet());

        Assert.assertEquals(Collections.singletonList("alpha"), missing);
    }

    @Test
    public void planMissingSessionNamesIncludesAlwaysSessionMissingAfterSessionReloadRebuild() {
        List<String> liveSessionNamesAfterReload = Arrays.asList(
            "https://example.test/a", "https://example.test/b");

        List<String> missing = planner.planMissingSessionNames(
            Collections.singletonList("myalways"), liveSessionNamesAfterReload, Collections.emptySet(), Collections.emptySet());

        Assert.assertEquals(Collections.singletonList("myalways"), missing);
    }

    @Test
    public void planMissingSessionNamesSkipsNamesTheOwnerHasHidden() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Collections.emptyList(), Collections.singleton("alpha"), Collections.emptySet());

        Assert.assertEquals(Collections.singletonList("beta"), missing);
    }
}
