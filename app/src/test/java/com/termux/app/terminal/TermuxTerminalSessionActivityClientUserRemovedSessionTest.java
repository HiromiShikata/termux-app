package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionLoadResult;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.app.terminal.session.FinishedSessionEnterAction;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionActivityClientUserRemovedSessionTest {

    private static final String GITHUB_SESSION_NAME =
        "https://github.com/HiromiShikata/termux-app/issues/2024";

    private static final String OTHER_GITHUB_SESSION_NAME =
        "https://github.com/HiromiShikata/termux-app/issues/9999";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
    }

    @Test
    public void deletingBareLeftoverSessionSuppressesItsReconnect() throws Exception {
        TermuxSession leftover = session("google logon");
        shellManager.mTermuxSessions.add(leftover);

        assertTrue(activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(leftover.getTerminalSession()).isReconnect());

        activity.getTermuxTerminalSessionClient().deleteSession(leftover.getTerminalSession());

        assertTrue(preferences.isSessionUserRemoved("google logon"));

        TerminalSession stillHostAlive = new TerminalSession(null, null, null, null, null, null);
        stillHostAlive.mSessionName = "google logon";
        assertFalse(activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(stillHostAlive).isReconnect());
    }

    @Test
    public void deletingAlwaysPresentSessionSuppressesItsReconnect() throws Exception {
        preferences.setAlwaysNaSessionNames("secretary");
        TermuxSession secretary = session("secretary");
        shellManager.mTermuxSessions.add(secretary);

        activity.getTermuxTerminalSessionClient().deleteSession(secretary.getTerminalSession());

        assertTrue("a deletion is a deletion: an always-present session the owner deleted must be "
                + "recorded as removed, so that no restore path creates it again on its own",
            preferences.isSessionUserRemoved("secretary"));

        TerminalSession recreatedCandidate = new TerminalSession(null, null, null, null, null, null);
        recreatedCandidate.mSessionName = "secretary";
        FinishedSessionEnterAction action = activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(recreatedCandidate);
        assertFalse("an always-present session the owner deleted must not be reconnected on its own",
            action.isReconnect());
    }

    @Test
    public void decideFinishedSessionEnterActionRemovesGithubSessionAbsentFromPublishedList()
            throws Exception {
        markPublishedSessionListLoaded(Collections.singletonList(OTHER_GITHUB_SESSION_NAME));
        preferences.setRemoveGithubSessionsNotInList(true);

        TerminalSession finishedSession = new TerminalSession(null, null, null, null, null, null);
        finishedSession.mSessionName = GITHUB_SESSION_NAME;

        FinishedSessionEnterAction action = activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(finishedSession);

        assertFalse("a GitHub-URL session absent from the published list must not be reconnected once "
                + "the list has been fetched at least once and the removal preference is enabled",
            action.isReconnect());
        assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
    }

    @Test
    public void decideFinishedSessionEnterActionReconnectsGithubSessionPresentInPublishedList()
            throws Exception {
        markPublishedSessionListLoaded(Collections.singletonList(GITHUB_SESSION_NAME));
        preferences.setRemoveGithubSessionsNotInList(true);

        TerminalSession finishedSession = new TerminalSession(null, null, null, null, null, null);
        finishedSession.mSessionName = GITHUB_SESSION_NAME;

        FinishedSessionEnterAction action = activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(finishedSession);

        assertTrue("a GitHub-URL session present in the published list must keep reconnecting exactly "
                + "as today",
            action.isReconnect());
    }

    @Test
    public void reconnectFinishedSessionInPlaceDoesNotRecreateGithubSessionAbsentFromPublishedList()
            throws Exception {
        markPublishedSessionListLoaded(Collections.singletonList(OTHER_GITHUB_SESSION_NAME));
        preferences.setRemoveGithubSessionsNotInList(true);

        TermuxSession finished = session(GITHUB_SESSION_NAME);
        shellManager.mTermuxSessions.add(finished);

        boolean reconnected = activity.getTermuxTerminalSessionClient()
            .reconnectFinishedSessionInPlace(finished.getTerminalSession(), null);

        assertFalse("a GitHub-URL session absent from the published list must not be recreated through "
                + "the in-place reconnect path shared by Enter-key reconnect, tap-to-switch, "
                + "tap-to-retry, and timeout-retry, consistent with how an explicitly user-removed "
                + "session already cannot be reconnected through any of those paths today",
            reconnected);
    }

    private void markPublishedSessionListLoaded(List<String> sessionNamesInList) throws Exception {
        Field repositoryField = TermuxActivity.class.getDeclaredField("mSessionDefinitionRepository");
        repositoryField.setAccessible(true);
        Object repository = repositoryField.get(activity);
        SessionDefinitionEntry entry = new SessionDefinitionEntry("group", "entry", sessionNamesInList);
        SessionDefinitionLoadResult result = new SessionDefinitionLoadResult(
            Collections.singletonList(entry), 1, Collections.emptyList());
        set(repository, SessionDefinitionRepository.class, "result", result);
        set(repository, SessionDefinitionRepository.class, "loaded", true);
    }

    private TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
