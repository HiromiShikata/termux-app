package com.termux.app.sessiondefinition;

import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionDefinitionCapCountPlannerTest {

    private final SessionDefinitionCapCountPlanner planner = new SessionDefinitionCapCountPlanner();

    @Test
    public void theDefaultCapMatchesTheProcessCountAndroidAllowsTheApp() {
        Assert.assertEquals("every session counted toward this cap holds a forked shell process, and"
                + " Android kills those processes once their count passes the ceiling it enforces,"
                + " whose documented default is 32",
            32, TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS);
    }

    @Test
    public void countsUpToSixtyFourAliveSessionsTowardCap() {
        int aliveCount = 64;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }

        Assert.assertEquals(aliveCount, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void sessionsUpToTheDefaultCapFitAndTheNextOneIsDropped() {
        int configuredLimit = TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;

        SessionDefinitionLimitPlan planAtCap = SessionDefinitionLimitPlan.forCapacity(
            configuredLimit, 0, configuredLimit);
        Assert.assertFalse(planAtCap.exceedsLimit());
        Assert.assertEquals(configuredLimit, planAtCap.getSessionsToCreateCount());

        SessionDefinitionLimitPlan planBeyondCap = SessionDefinitionLimitPlan.forCapacity(
            configuredLimit + 1, 0, configuredLimit);
        Assert.assertTrue(planBeyondCap.exceedsLimit());
        Assert.assertEquals(configuredLimit, planBeyondCap.getSessionsToCreateCount());
        Assert.assertEquals(1, planBeyondCap.getDroppedSessionCount());
    }

    @Test
    public void countsEveryAliveSessionIncludingUnnamedOnesWhileNothingIsHidden() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", true),
            countedSession("host-b", true),
            countedSession(null, true));

        Assert.assertEquals(3, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void doesNotCountARunningSessionWhoseNameIsHidden() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", true),
            countedSession("host-hidden", true),
            countedSession("host-b", true));

        int capCount = planner.countSessionsTowardCap(countedSessions,
            Collections.singleton("host-hidden"));

        Assert.assertEquals("a hidden session holds no shell process, no terminal emulator and no live "
                + "session object at all, so it occupies no slot under the session cap; counting it "
                + "would keep a session the owner still wants from being created, and the two sessions "
                + "that are not hidden must still be counted so this does not pass by refusing "
                + "everything", 2, capCount);
    }

    @Test
    public void excludesDeadSessionsWithoutAName() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void excludesDeadNonReconnectableSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", false),
            countedSession("host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void excludesDeadReconnectableDefinitionBackedSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("autossh-host-a", false),
            countedSession("autossh-host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void countsAliveSessionsButNotDeadReconnectableOrOrphanSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("alive", true),
            countedSession("dead-reconnectable", false),
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(1, planner.countSessionsTowardCap(countedSessions, Collections.emptySet()));
    }

    @Test
    public void capCountReflectsLiveSessionsWhenDeadOrphansAreMixedIn() {
        int aliveCount = 10;
        int deadOrphanCount = 13;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadOrphanCount; i++) {
            countedSessions.add(countedSession(null, false));
        }

        int capCount = planner.countSessionsTowardCap(countedSessions, Collections.emptySet());

        Assert.assertEquals(aliveCount, capCount);
    }

    @Test
    public void deadSessionsDoNotBlockCreationWhileLiveSessionsAreUnderTheCap() {
        int configuredLimit = TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;
        int aliveCount = 20;
        int deadReconnectableCount = 15;

        Assert.assertTrue(aliveCount < configuredLimit);
        Assert.assertTrue(configuredLimit <= aliveCount + deadReconnectableCount);

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadReconnectableCount; i++) {
            countedSessions.add(countedSession("autossh-dead-" + i, false));
        }

        int capCount = planner.countSessionsTowardCap(countedSessions, Collections.emptySet());

        Assert.assertEquals(aliveCount, capCount);
        Assert.assertTrue(capCount < configuredLimit);
    }

    @Test
    public void liveSessionsAtTheCapStillBlockCreation() {
        int configuredLimit = TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < configuredLimit; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }

        int capCount = planner.countSessionsTowardCap(countedSessions, Collections.emptySet());

        Assert.assertEquals(configuredLimit, capCount);
        Assert.assertTrue(capCount >= configuredLimit);
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

        int cappedCountExcludingOrphans = planner.countSessionsTowardCap(countedSessions, Collections.emptySet());
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
