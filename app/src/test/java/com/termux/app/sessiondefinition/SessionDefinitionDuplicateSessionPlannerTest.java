package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionDefinitionDuplicateSessionPlannerTest {

    private final SessionDefinitionDuplicateSessionPlanner planner =
        new SessionDefinitionDuplicateSessionPlanner();

    @Test
    public void removesNothingWhenEveryNameIsUnique() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            liveSession("a", true),
            liveSession("b", true),
            liveSession("c", false));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertTrue(indexesToRemove.isEmpty());
    }

    @Test
    public void removesTheDuplicateOccurrenceOfARepeatedName() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            liveSession("a", true),
            liveSession("b", true),
            liveSession("a", true));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertEquals(Arrays.asList(2), indexesToRemove);
    }

    @Test
    public void keepsTheRunningInstanceWhenAnEarlierInstanceIsNotRunning() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            liveSession("a", false),
            liveSession("a", true));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertEquals(Arrays.asList(0), indexesToRemove);
    }

    @Test
    public void keepsTheFirstInstanceWhenNeitherDuplicateIsRunning() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            liveSession("a", false),
            liveSession("a", false));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertEquals(Arrays.asList(1), indexesToRemove);
    }

    @Test
    public void reconcilesEveryDuplicateSoEachNameIsCountedOnce() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            liveSessions.add(liveSession("session-" + i, true));
        }
        liveSessions.add(liveSession("session-0", true));
        liveSessions.add(liveSession("session-5", true));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertEquals(2, indexesToRemove.size());
        Assert.assertEquals(liveSessions.size() - indexesToRemove.size(), distinctNameCount(liveSessions));
    }

    @Test
    public void ignoresNullNamesWithoutCollapsingThemTogether() {
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions = Arrays.asList(
            liveSession(null, true),
            liveSession(null, true));

        List<Integer> indexesToRemove = planner.planDuplicateSessionIndexesToRemove(liveSessions);

        Assert.assertTrue(indexesToRemove.isEmpty());
    }

    private static int distinctNameCount(
        List<SessionDefinitionDuplicateSessionPlanner.LiveSession> liveSessions) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (SessionDefinitionDuplicateSessionPlanner.LiveSession liveSession : liveSessions) {
            names.add(liveSession.getName());
        }
        return names.size();
    }

    private static SessionDefinitionDuplicateSessionPlanner.LiveSession liveSession(String name, boolean running) {
        return new SessionDefinitionDuplicateSessionPlanner.LiveSession(name, running);
    }
}
