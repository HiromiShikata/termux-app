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
            Arrays.asList("alpha", "beta"), Collections.emptyList());

        Assert.assertEquals(Arrays.asList("alpha", "beta"), missing);
    }

    @Test
    public void planMissingSessionNamesSkipsNamesAlreadyLive() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Collections.singletonList("alpha"));

        Assert.assertEquals(Collections.singletonList("beta"), missing);
    }

    @Test
    public void planMissingSessionNamesReturnsEmptyWhenAllConfiguredNamesAreLive() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList("alpha", "beta"), Arrays.asList("alpha", "beta"));

        Assert.assertTrue(missing.isEmpty());
    }

    @Test
    public void planMissingSessionNamesReturnsEmptyWhenNoNamesAreConfigured() {
        List<String> missing = planner.planMissingSessionNames(
            Collections.emptyList(), Collections.singletonList("alpha"));

        Assert.assertTrue(missing.isEmpty());
    }

    @Test
    public void planMissingSessionNamesDeduplicatesAndTrimsConfiguredNames() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList(" alpha ", "alpha", "beta"), Collections.emptyList());

        Assert.assertEquals(Arrays.asList("alpha", "beta"), missing);
    }

    @Test
    public void planMissingSessionNamesIgnoresNullAndBlankConfiguredNames() {
        List<String> missing = planner.planMissingSessionNames(
            Arrays.asList(null, "   ", "alpha"), Collections.emptyList());

        Assert.assertEquals(Collections.singletonList("alpha"), missing);
    }
}
