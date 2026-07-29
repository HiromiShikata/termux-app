package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;
import com.termux.app.terminal.session.TransientCommandSessionName;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionActivityClientResetHostSessionTest {

    private static final String TARGET_SESSION_NAME = "work-session";

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
        preferences.setAutosshCommand("autossh -M 0 {name}");
        preferences.setResetSessionCommand("ssh gateway /opt/reset.sh {name}");
        preferences.setSessionDefinitionMaxSessions(10);

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession targetSession = liveSession(TARGET_SESSION_NAME);
        shellManager.mTermuxSessions.add(targetSession);
        terminalView.mTermSession = targetSession.getTerminalSession();
    }

    @Test
    public void exitedResetCommandSessionIsRemovedFromTheSessionListInsteadOfLingering() throws Exception {
        TermuxSession exitedResetSession = deadSession(resetSessionName());
        shellManager.mTermuxSessions.add(exitedResetSession);
        assertNotNull(service.getTermuxSessionForSessionName(resetSessionName()));

        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(exitedResetSession.getTerminalSession());

        assertNull("a reset command session that has exited must not linger in the session list",
            service.getTermuxSessionForSessionName(resetSessionName()));
    }

    @Test
    public void exitedResetCommandSessionIsNeverReconnectedEvenThoughAConnectTemplateIsConfigured() throws Exception {
        TermuxSession exitedResetSession = deadSession(resetSessionName());

        assertFalse("a connect template is configured, which is the normal state for this feature",
            preferences.getAutosshCommand().isEmpty());
        assertFalse("an exited reset command session must never be treated as reconnectable",
            activity.getTermuxTerminalSessionClient()
                .decideFinishedSessionEnterAction(exitedResetSession.getTerminalSession())
                .isReconnect());
    }

    @Test
    public void backgroundReconnectPlanningSkipsTheExitedResetCommandSessionButKeepsOrdinarySessions() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession(resetSessionName(), false),
            new DeadSessionReconnectPlanner.CandidateSession("dead-ordinary-session", false));

        List<String> plannedSessionNames = new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, preferences.getAutosshCommand());

        assertEquals(Collections.singletonList("dead-ordinary-session"), plannedSessionNames);
    }

    @Test
    public void exitedResetCommandSessionIsNotPersistedAndSoCannotSurviveAnAppRestart() throws Exception {
        int sessionCountBeforeReset = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().resetHostSession(targetTerminalSession());

        TermuxSession resetSession = service.getTermuxSessionForSessionName(resetSessionName());
        assertNotNull("the reset command session must actually be created by resetHostSession", resetSession);
        assertEquals(sessionCountBeforeReset + 1, service.getTermuxSessionsSize());
        assertTrue("the created session must run the composed reset command",
            Arrays.asList(resetSession.getExecutionCommand().arguments)
                .contains("ssh gateway /opt/reset.sh 'work-session'"));

        TerminalSession resetTerminalSession = resetSession.getTerminalSession();
        markProcessExited(resetTerminalSession);
        activity.getTermuxTerminalSessionClient().onSessionFinished(resetTerminalSession);

        boolean reconnected = activity.getTermuxTerminalSessionClient()
            .reconnectFinishedSessionInPlace(resetTerminalSession, null);

        assertFalse("the reset command session must never reach the persisted session store, "
                + "because a persisted record would resurrect it on the next app start",
            preferences.getPersistedSessions().contains(TransientCommandSessionName.RESET_PREFIX));
        assertFalse("the exited reset command session must never be reconnected into a live ssh session",
            reconnected);
    }

    @Test
    public void repeatedResetTapsRevealTheRunningResetSessionInsteadOfCreatingAnother() throws Exception {
        shellManager.mTermuxSessions.add(liveSession(resetSessionName()));
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().resetHostSession(targetTerminalSession());

        assertEquals("a second reset tap must not create another session while the first is still running",
            sessionCountBefore, service.getTermuxSessionsSize());
    }

    @Test
    public void resetIsRejectedWhenTheMaximumSessionCountIsAlreadyReached() throws Exception {
        preferences.setSessionDefinitionMaxSessions(2);
        shellManager.mTermuxSessions.add(liveSession("another-live-session"));
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().resetHostSession(targetTerminalSession());

        assertEquals("reset must honour the maximum session cap the sibling session-creating actions honour",
            sessionCountBefore, service.getTermuxSessionsSize());
    }

    @Test
    public void resetCreatesNothingWhenNoCommandTemplateIsConfigured() throws Exception {
        preferences.setResetSessionCommand("");
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().resetHostSession(targetTerminalSession());

        assertEquals(sessionCountBefore, service.getTermuxSessionsSize());
        assertNull(service.getTermuxSessionForSessionName(resetSessionName()));
    }

    @Test
    public void ordinaryFinishedSessionIsStillRetainedForItsReconnectPrompt() throws Exception {
        TermuxSession exitedOrdinarySession = deadSession("dead-ordinary-session");
        shellManager.mTermuxSessions.add(exitedOrdinarySession);

        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(exitedOrdinarySession.getTerminalSession());

        assertNotNull("only transient command sessions may be auto-removed on exit",
            service.getTermuxSessionForSessionName("dead-ordinary-session"));
        assertTrue(activity.getTermuxTerminalSessionClient()
            .decideFinishedSessionEnterAction(exitedOrdinarySession.getTerminalSession())
            .isReconnect());
    }

    private String resetSessionName() {
        return TransientCommandSessionName.forResetOfSession(TARGET_SESSION_NAME);
    }

    private TerminalSession targetTerminalSession() {
        TermuxSession targetSession = service.getTermuxSessionForSessionName(TARGET_SESSION_NAME);
        assertNotNull(targetSession);
        return targetSession.getTerminalSession();
    }

    private TermuxSession liveSession(String name) throws Exception {
        return termuxSession(name, false);
    }

    private TermuxSession deadSession(String name) throws Exception {
        return termuxSession(name, true);
    }

    private void markProcessExited(TerminalSession terminalSession) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
    }

    private TermuxSession termuxSession(String name, boolean exited) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        if (exited) {
            markProcessExited(terminalSession);
        }
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
