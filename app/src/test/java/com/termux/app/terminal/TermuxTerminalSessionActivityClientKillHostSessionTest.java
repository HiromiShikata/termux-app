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
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionActivityClientKillHostSessionTest {

    private static final String TARGET_SESSION_NAME = "work-session";

    private static final String KILL_TEMPLATE = "ssh gateway tmux kill-session -t {name}";

    private static final String COMPOSED_KILL_COMMAND = "ssh gateway tmux kill-session -t 'work-session'";

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
        preferences.setKillSessionCommand(KILL_TEMPLATE);
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setPersistedSessions("");

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
    public void killStartsTheCommandSessionUnderItsTransientNameRunningTheConfiguredTemplate() {
        int sessionCountBeforeKill = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        TermuxSession killSession = service.getTermuxSessionForSessionName(killSessionName());
        assertNotNull("the kill command session must actually be created by killHostSession", killSession);
        assertEquals(sessionCountBeforeKill + 1, service.getTermuxSessionsSize());
        assertTrue("the created session must run the composed kill command",
            Arrays.asList(killSession.getExecutionCommand().arguments).contains(COMPOSED_KILL_COMMAND));
    }

    @Test
    public void killCommandRunsAsItsOwnShellSessionSoItSurvivesTheSessionBeingKilled() {
        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        TermuxSession killSession = service.getTermuxSessionForSessionName(killSessionName());
        assertNotNull(killSession);
        List<String> arguments = Arrays.asList(killSession.getExecutionCommand().arguments);
        assertEquals(Arrays.asList("-c", COMPOSED_KILL_COMMAND),
            arguments.subList(arguments.size() - 2, arguments.size()));
        assertTrue(killSession.getExecutionCommand().executable.endsWith("/sh"));
    }

    @Test
    public void killCommandSessionIsNotPersistedAndSoCannotSurviveAnAppRestart() {
        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        assertNotNull(service.getTermuxSessionForSessionName(killSessionName()));
        assertFalse("the kill command session must never reach the persisted session store, "
                + "because a persisted record would resurrect it on the next app start",
            preferences.getPersistedSessions().contains(TransientCommandSessionName.KILL_PREFIX));
    }

    @Test
    public void repeatedKillTapsRevealTheRunningKillSessionInsteadOfCreatingAnother() throws Exception {
        shellManager.mTermuxSessions.add(liveSession(killSessionName()));
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        assertEquals("a second kill tap must not create another session while the first is still running",
            sessionCountBefore, service.getTermuxSessionsSize());
    }

    @Test
    public void killIsRejectedWhenTheMaximumSessionCountIsAlreadyReached() throws Exception {
        preferences.setSessionDefinitionMaxSessions(2);
        shellManager.mTermuxSessions.add(liveSession("another-live-session"));
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        assertEquals("kill must honour the maximum session cap the sibling session-creating actions honour",
            sessionCountBefore, service.getTermuxSessionsSize());
        assertNull(service.getTermuxSessionForSessionName(killSessionName()));
    }

    @Test
    public void killCreatesNothingWhenNoCommandTemplateIsConfigured() {
        preferences.setKillSessionCommand("");
        int sessionCountBefore = service.getTermuxSessionsSize();

        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        assertEquals(sessionCountBefore, service.getTermuxSessionsSize());
        assertNull(service.getTermuxSessionForSessionName(killSessionName()));
    }

    @Test
    public void theSessionBeingKilledIsClosedOnlyAfterTheHostKillCommandHasHadTimeToReachTheHost() {
        activity.getTermuxTerminalSessionClient().killHostSession(targetTerminalSession());

        assertNotNull("the local session must stay until the host has had time to act",
            service.getTermuxSessionForSessionName(TARGET_SESSION_NAME));

        ShadowLooper.idleMainLooper(HostTmuxSessionKiller.HOST_KILL_TRANSIT_GRACE_MILLIS,
            TimeUnit.MILLISECONDS);

        assertNull("the local session must be closed once the grace period has elapsed",
            service.getTermuxSessionForSessionName(TARGET_SESSION_NAME));
    }

    @Test
    public void exitedKillCommandSessionIsRemovedFromTheSessionListInsteadOfLingering() throws Exception {
        TermuxSession exitedKillSession = deadSession(killSessionName());
        shellManager.mTermuxSessions.add(exitedKillSession);
        assertNotNull(service.getTermuxSessionForSessionName(killSessionName()));

        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(exitedKillSession.getTerminalSession());

        assertNull("a kill command session that has exited must not linger in the session list",
            service.getTermuxSessionForSessionName(killSessionName()));
    }

    @Test
    public void exitedKillCommandSessionIsRemovedBeforeAnyExitedToastCanBeShownForIt() throws Exception {
        set(activity, TermuxActivity.class, "mIsVisible", true);
        TermuxSession exitedKillSession = deadSession(killSessionName());
        shellManager.mTermuxSessions.add(exitedKillSession);
        ShadowToast.reset();

        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(exitedKillSession.getTerminalSession());

        assertEquals("a command session is an implementation detail and must never announce its own exit",
            0, ShadowToast.shownToastCount());
        assertNull(service.getTermuxSessionForSessionName(killSessionName()));
    }

    @Test
    public void exitedOrdinarySessionStillAnnouncesItsExitAndIsRetainedForItsReconnectPrompt() throws Exception {
        set(activity, TermuxActivity.class, "mIsVisible", true);
        TermuxSession exitedOrdinarySession = deadSession("dead-ordinary-session");
        shellManager.mTermuxSessions.add(exitedOrdinarySession);
        ShadowToast.reset();

        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(exitedOrdinarySession.getTerminalSession());

        assertEquals(1, ShadowToast.shownToastCount());
        assertNotNull("only transient command sessions may be auto-removed on exit",
            service.getTermuxSessionForSessionName("dead-ordinary-session"));
    }

    private String killSessionName() {
        return TransientCommandSessionName.forKillOfSession(TARGET_SESSION_NAME);
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
