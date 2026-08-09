package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.SessionReconnectPacer;
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
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ExitedSessionIsReconnectedAtOnceTest {

    private static final String DISPLAYED_SESSION_NAME = "displayed-session";

    private static final String BACKGROUND_SESSION_NAME = "background-session";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;

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

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setPersistedSessions("");

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();

        ShadowToast.reset();
    }

    @Test
    public void aSessionWhoseShellExitedIsReconnectedWithoutWaitingForTheBackgroundScan() throws Exception {
        TermuxSession exitedSession = deadSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(exitedSession);
        TerminalSession exitedTerminalSession = exitedSession.getTerminalSession();

        activity.getTermuxTerminalSessionClient().onSessionFinished(exitedTerminalSession);
        idlePastTheReconnectPacer();

        TermuxSession sessionUnderTheSameName =
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull("the session must still be in the list under its own name after it is reconnected",
            sessionUnderTheSameName);
        assertNotSame("a session whose shell process has exited must be reconnected the moment it exits,"
                + " otherwise it stays dead until the next background scan, which runs minutes later",
            exitedTerminalSession, sessionUnderTheSameName.getTerminalSession());
    }

    @Test
    public void aSessionWhoseShellExitedIsNotAnnouncedWithAToast() throws Exception {
        TermuxSession exitedSession = deadSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(exitedSession);

        activity.getTermuxTerminalSessionClient().onSessionFinished(exitedSession.getTerminalSession());
        idlePastTheReconnectPacer();

        assertEquals("a session that exits is reconnected rather than reported, and the toast covers the"
                + " terminal output the owner is reading, so nothing may be shown for it",
            0, ShadowToast.shownToastCount());
    }

    @Test
    public void aSessionThatExitsAgainImmediatelyIsLeftToTheBackgroundScanInsteadOfLooping() throws Exception {
        TermuxSession exitedSession = deadSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(exitedSession);

        activity.getTermuxTerminalSessionClient().onSessionFinished(exitedSession.getTerminalSession());
        idlePastTheReconnectPacer();

        TermuxSession firstReplacement = service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull(firstReplacement);
        TerminalSession firstReplacementTerminalSession = firstReplacement.getTerminalSession();
        markProcessExited(firstReplacementTerminalSession);

        activity.getTermuxTerminalSessionClient().onSessionFinished(firstReplacementTerminalSession);
        idlePastTheReconnectPacer();

        TermuxSession sessionUnderTheSameName =
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull(sessionUnderTheSameName);
        assertSame("a command that fails the instant it starts exits again immediately, so reconnecting"
                + " every exit without spacing the repeats rebuilds the session in a tight loop",
            firstReplacementTerminalSession, sessionUnderTheSameName.getTerminalSession());
    }

    private void idlePastTheReconnectPacer() {
        ShadowLooper.idleMainLooper(
            SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS * 2, TimeUnit.MILLISECONDS);
    }

    private TermuxSession liveSession(String name) throws Exception {
        return termuxSession(name, false);
    }

    private TermuxSession deadSession(String name) throws Exception {
        return termuxSession(name, true);
    }

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

    private void markProcessRunning(TerminalSession terminalSession) throws Exception {
        shellPidField().setInt(terminalSession, RUNNING_SHELL_PROCESS_PID);
    }

    private void markProcessExited(TerminalSession terminalSession) throws Exception {
        shellPidField().setInt(terminalSession, -1);
    }

    private Field shellPidField() throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        return shellPid;
    }

    private TermuxSession termuxSession(String name, boolean exited) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        if (exited) {
            markProcessExited(terminalSession);
        } else {
            markProcessRunning(terminalSession);
        }
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
