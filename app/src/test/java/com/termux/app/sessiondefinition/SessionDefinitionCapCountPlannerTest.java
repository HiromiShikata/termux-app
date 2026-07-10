package com.termux.app.sessiondefinition;

import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionDefinitionCapCountPlannerTest {

    private final SessionDefinitionCapCountPlanner planner = new SessionDefinitionCapCountPlanner();

    @Test
    public void defaultCapIsSixtyFour() {
        Assert.assertEquals(64,
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS);
    }

    @Test
    public void countsUpToSixtyFourAliveSessionsTowardCap() {
        int aliveCount = 64;

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }

        Assert.assertEquals(aliveCount, planner.countSessionsTowardCap(countedSessions));
    }

    @Test
    public void sixtyFourSessionsFitTheDefaultCapAndTheSixtyFifthIsDropped() {
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
    public void countsEveryAliveSessionIncludingHiddenAndUnnamedOnes() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", true),
            countedSession("host-b", true),
            countedSession(null, true));

        Assert.assertEquals(3, planner.countSessionsTowardCap(countedSessions));
    }

    @Test
    public void excludesDeadSessionsWithoutAName() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions));
    }

    @Test
    public void excludesDeadNonReconnectableSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("host-a", false),
            countedSession("host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions));
    }

    @Test
    public void excludesDeadReconnectableDefinitionBackedSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("autossh-host-a", false),
            countedSession("autossh-host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(countedSessions));
    }

    @Test
    public void countsAliveSessionsButNotDeadReconnectableOrOrphanSessions() {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = Arrays.asList(
            countedSession("alive", true),
            countedSession("dead-reconnectable", false),
            countedSession(null, false),
            countedSession("   ", false));

        Assert.assertEquals(1, planner.countSessionsTowardCap(countedSessions));
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

        int capCount = planner.countSessionsTowardCap(countedSessions);

        Assert.assertEquals(aliveCount, capCount);
    }

    @Test
    public void deadSessionsDoNotBlockCreationWhileLiveSessionsAreUnderTheCap() {
        int configuredLimit = TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;
        int aliveCount = 39;
        int deadReconnectableCount = 25;

        Assert.assertTrue(aliveCount < configuredLimit);
        Assert.assertTrue(configuredLimit <= aliveCount + deadReconnectableCount);

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            countedSessions.add(countedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadReconnectableCount; i++) {
            countedSessions.add(countedSession("autossh-dead-" + i, false));
        }

        int capCount = planner.countSessionsTowardCap(countedSessions);

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

        int capCount = planner.countSessionsTowardCap(countedSessions);

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

        int cappedCountExcludingOrphans = planner.countSessionsTowardCap(countedSessions);
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
