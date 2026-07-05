package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionDefinitionCapCountPlannerTest {

    private static final String RECONNECTABLE_TEMPLATE = "autossh {name}";

    private final SessionDefinitionCapCountPlanner planner = new SessionDefinitionCapCountPlanner();

    @Test
    public void countsEveryAliveSession() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", true),
            countedSession("host-b", true),
            countedSession(null, true));

        Assert.assertEquals(3, planner.countSessionsTowardCap(countedSessions, ""));
    }

    @Test
    public void excludesDeadSessionsWithoutAName() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions, RECONNECTABLE_TEMPLATE));
    }

    @Test
    public void excludesDeadNonReconnectableSessionsWhenNoAutosshTemplateIsConfigured() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", false),
            countedSession("host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions, ""));
    }

    @Test
    public void countsDeadReconnectableDefinitionBackedSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", false),
            countedSession("host-b", false));

        Assert.assertEquals(2, planner.countSessionsTowardCap(countedSessions, RECONNECTABLE_TEMPLATE));
    }

    @Test
    public void countsAliveSessionsAndDeadReconnectableSessionsButNotDeadOrphans() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("alive", true),
            countedSession("dead-reconnectable", false),
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(2, planner.countSessionsTowardCap(countedSessions, RECONNECTABLE_TEMPLATE));
    }

    @Test
    public void capCountReflectsRealSessionsWhenDeadOrphansAreMixedIn() {
        int aliveCount = 10;
        int deadOrphanCount = 13;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadOrphanCount; i++) {
            countedSessions.add(countedSession(null, false));
        }

        int capCount = planner.countSessionsTowardCap(countedSessions, "");

        Assert.assertEquals(aliveCount, capCount);
    }

    @Test
    public void reloadNoLongerErrorsWhenDeadOrphansPreviouslyExhaustedTheCap() {
        int configuredLimit = 32;
        int aliveCount = 10;
        int deadOrphanCount = 13;
        int requestedShownSessions = aliveCount;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadOrphanCount; i++) {
            countedSessions.add(countedSession(null, false));
        }

        int cappedCountExcludingOrphans = planner.countSessionsTowardCap(countedSessions, "");
        SessionDefinitionLimitPlan planExcludingOrphans = SessionDefinitionLimitPlan.forCapacity(
            requestedShownSessions, cappedCountExcludingOrphans, configuredLimit);
        Assert.assertFalse(planExcludingOrphans.exceedsLimit());
        Assert.assertEquals(requestedShownSessions, planExcludingOrphans.getSessionsToCreateCount());

        int cappedCountIncludingOrphans = aliveCount + deadOrphanCount;
        SessionDefinitionLimitPlan planIncludingOrphans = SessionDefinitionLimitPlan.forCapacity(
            requestedShownSessions, cappedCountIncludingOrphans, configuredLimit);
        Assert.assertTrue(planIncludingOrphans.exceedsLimit());
    }

    private static SessionDefinitionCapCountPlanner.CountedSession countedSession(String name, boolean running) {
        return new SessionDefinitionCapCountPlanner.CountedSession(name, running);
    }
}
