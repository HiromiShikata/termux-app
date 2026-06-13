package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SessionDefinitionExistingSessionFilterTest {

    @Test
    public void selectSessionsToCreateReturnsAllSessionsWhenNoneExist() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("beta", "command-beta"));

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, Collections.emptySet());

        Assert.assertEquals(2, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
        Assert.assertEquals("beta", sessionsToCreate.get(1).getName());
    }

    @Test
    public void selectSessionsToCreateSkipsSessionsThatAlreadyExist() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("beta", "command-beta"),
            new SessionDefinitionPlannedSession("gamma", null));
        Set<String> existingSessionNames = new HashSet<>(Arrays.asList("alpha", "gamma"));

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, existingSessionNames);

        Assert.assertEquals(1, sessionsToCreate.size());
        Assert.assertEquals("beta", sessionsToCreate.get(0).getName());
    }

    @Test
    public void selectSessionsToCreateSkipsIntraBatchDuplicateNames() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("alpha", "command-alpha"),
            new SessionDefinitionPlannedSession("beta", null));

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(plannedSessions, Collections.emptySet());

        Assert.assertEquals(2, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
        Assert.assertNull(sessionsToCreate.get(0).getCommand());
        Assert.assertEquals("beta", sessionsToCreate.get(1).getName());
    }
}
