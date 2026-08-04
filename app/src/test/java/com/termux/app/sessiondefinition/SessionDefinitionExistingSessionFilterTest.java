package com.termux.app.sessiondefinition;

import com.termux.shared.termux.settings.preferences.UserRemovedSessionHideWindow;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionDefinitionExistingSessionFilterTest {

    private static final long NOW_MILLIS = 1_700_000_000_000L;

    private static final Map<String, Long> NO_SESSION_THE_OWNER_DELETED = Collections.emptyMap();

    @Test
    public void selectSessionsToCreateReturnsAllSessionsWhenNoneExist() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("beta", "command-beta"));

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.emptySet(),
                NO_SESSION_THE_OWNER_DELETED, NOW_MILLIS);

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
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, existingSessionNames, Collections.emptySet(),
                NO_SESSION_THE_OWNER_DELETED, NOW_MILLIS);

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
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.emptySet(),
                NO_SESSION_THE_OWNER_DELETED, NOW_MILLIS);

        Assert.assertEquals(2, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
        Assert.assertNull(sessionsToCreate.get(0).getCommand());
        Assert.assertEquals("beta", sessionsToCreate.get(1).getName());
    }

    @Test
    public void selectSessionsToCreateSkipsSessionsTheOwnerHasHidden() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("beta", "command-beta"));
        Set<String> hiddenSessionNames = Collections.singleton("beta");

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), hiddenSessionNames,
                NO_SESSION_THE_OWNER_DELETED, NOW_MILLIS);

        Assert.assertEquals(1, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
    }

    @Test
    public void selectSessionsToCreateStillReturnsANameThatIsAbsentAndNotHidden() {
        List<SessionDefinitionPlannedSession> plannedSessions = Collections.singletonList(
            new SessionDefinitionPlannedSession("alpha", null));

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.singleton("beta"),
                NO_SESSION_THE_OWNER_DELETED, NOW_MILLIS);

        Assert.assertEquals(1, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
    }

    @Test
    public void selectSessionsToCreateSkipsASessionTheOwnerDeletedOneMinuteAgo() {
        List<SessionDefinitionPlannedSession> plannedSessions = Arrays.asList(
            new SessionDefinitionPlannedSession("alpha", null),
            new SessionDefinitionPlannedSession("beta", "command-beta"));
        Map<String, Long> deletedByTheOwner = Collections.singletonMap("beta", NOW_MILLIS - 60_000L);

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.emptySet(),
                deletedByTheOwner, NOW_MILLIS);

        Assert.assertEquals("a session the owner deleted a minute ago must not be created again by a"
                + " definition load, because the published document keeps listing it until the next"
                + " management tool schedule cycle rewrites it",
            1, sessionsToCreate.size());
        Assert.assertEquals("alpha", sessionsToCreate.get(0).getName());
    }

    @Test
    public void selectSessionsToCreateReturnsASessionTheOwnerDeletedAFullFifteenMinutesAgo() {
        List<SessionDefinitionPlannedSession> plannedSessions = Collections.singletonList(
            new SessionDefinitionPlannedSession("beta", "command-beta"));
        Map<String, Long> deletedByTheOwner = Collections.singletonMap("beta",
            NOW_MILLIS - UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS);

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.emptySet(),
                deletedByTheOwner, NOW_MILLIS);

        Assert.assertEquals("the deletion hides the session for fifteen minutes and no longer, so once"
                + " that window has passed a definition load creates the name again exactly as it did"
                + " before the owner deleted it",
            1, sessionsToCreate.size());
        Assert.assertEquals("beta", sessionsToCreate.get(0).getName());
    }

    @Test
    public void selectSessionsToCreateSkipsASessionTheOwnerDeletedOneMillisecondInsideTheWindow() {
        List<SessionDefinitionPlannedSession> plannedSessions = Collections.singletonList(
            new SessionDefinitionPlannedSession("beta", "command-beta"));
        Map<String, Long> deletedByTheOwner = Collections.singletonMap("beta",
            NOW_MILLIS - UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS + 1L);

        List<SessionDefinitionPlannedSession> sessionsToCreate =
            SessionDefinitionExistingSessionFilter.selectSessionsToCreate(
                plannedSessions, Collections.emptySet(), Collections.emptySet(),
                deletedByTheOwner, NOW_MILLIS);

        Assert.assertTrue("the last millisecond of the fifteen minute window still hides the session,"
                + " otherwise the window would end early", sessionsToCreate.isEmpty());
    }
}
