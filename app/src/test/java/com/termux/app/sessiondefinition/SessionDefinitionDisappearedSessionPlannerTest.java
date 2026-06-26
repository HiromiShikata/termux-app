package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionDisappearedSessionPlannerTest {

    private final SessionDefinitionDisappearedSessionPlanner planner =
        new SessionDefinitionDisappearedSessionPlanner();

    private final List<SessionDefinitionEntry> configWithThreeSessions = Arrays.asList(
        new SessionDefinitionEntry("projectOne", "storyA",
            Arrays.asList("https://example.test/a", "https://example.test/b")),
        new SessionDefinitionEntry("projectOne", "storyB",
            Collections.singletonList("https://example.test/c")));

    @Test
    public void removesProjectSessionDroppedFromConfigEvenWithoutPreviousEntries() {
        List<SessionDefinitionEntry> reloadedConfig = Arrays.asList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")),
            new SessionDefinitionEntry("projectOne", "storyB",
                Collections.singletonList("https://example.test/c")));

        List<String> namesToRemove = planner.planSessionNamesToRemove(reloadedConfig,
            Collections.emptySet(), notRunning(
                "https://example.test/a", "https://example.test/b", "https://example.test/c"));

        assertEquals(Collections.singletonList("https://example.test/b"), namesToRemove);
    }

    @Test
    public void keepsAdHocLocalSessionsThatAreNotUrlNamed() {
        List<String> namesToRemove = planner.planSessionNamesToRemove(configWithThreeSessions,
            Collections.emptySet(), notRunning("adhoc-local", "another-local"));

        assertEquals(Collections.emptyList(), namesToRemove);
    }

    @Test
    public void keepsLiveConnectedSessionAbsentFromConfig() {
        List<SessionDefinitionDisappearedSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            new SessionDefinitionDisappearedSessionPlanner.LiveSession("https://example.test/a", false),
            new SessionDefinitionDisappearedSessionPlanner.LiveSession("https://example.test/gone", true));

        List<String> namesToRemove = planner.planSessionNamesToRemove(configWithThreeSessions,
            Collections.emptySet(), liveSessions);

        assertEquals(Collections.emptyList(), namesToRemove);
    }

    @Test
    public void keepsAlwaysPresentSessionAbsentFromConfig() {
        Set<String> alwaysPresentSessionNames = new LinkedHashSet<>(
            Collections.singletonList("https://example.test/leftover"));

        List<String> namesToRemove = planner.planSessionNamesToRemove(configWithThreeSessions,
            alwaysPresentSessionNames, notRunning("https://example.test/leftover"));

        assertEquals(Collections.emptyList(), namesToRemove);
    }

    @Test
    public void removesAllProjectSessionsWhenConfigBecomesEmpty() {
        List<String> namesToRemove = planner.planSessionNamesToRemove(Collections.emptyList(),
            Collections.emptySet(), notRunning(
                "adhoc-local", "https://example.test/a", "https://example.test/c"));

        assertEquals(Arrays.asList("https://example.test/a", "https://example.test/c"), namesToRemove);
    }

    private List<SessionDefinitionDisappearedSessionPlanner.LiveSession> notRunning(String... names) {
        List<SessionDefinitionDisappearedSessionPlanner.LiveSession> liveSessions =
            new java.util.ArrayList<>(names.length);
        for (String name : names) {
            liveSessions.add(new SessionDefinitionDisappearedSessionPlanner.LiveSession(name, false));
        }
        return liveSessions;
    }
}
