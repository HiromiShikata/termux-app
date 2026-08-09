package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ExitedConnectedSessionIsReconnectedAtOnceTest {

    private static final String DISPLAYED_SESSION_NAME = "displayed-session";

    private static final String BACKGROUND_SESSION_NAME = "background-session";

    private static final long BYTES_A_CONNECTED_SESSION_HAS_PROCESSED = 4096L;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TerminalView terminalView;

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

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession displayedSession = connectedSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();
    }

    @Test
    public void aSessionThatWasConnectedAndThenDiedIsReconnectedWithoutWaitingForTheBackgroundScan()
        throws Exception {
        TermuxSession diedSession = connectedThenDiedSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(diedSession);
        TerminalSession diedTerminalSession = diedSession.getTerminalSession();

        activity.getTermuxTerminalSessionClient().onSessionFinished(diedTerminalSession);
        idlePastTheReconnectPacer();

        TermuxSession sessionUnderTheSameName =
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull("the session must still be in the list under its own name after it is reconnected",
            sessionUnderTheSameName);
        assertNotSame("a session that was connected and whose shell process then died must be reconnected"
                + " the moment it dies, otherwise it stays dead until the next background scan, which runs"
                + " minutes later",
            diedTerminalSession, sessionUnderTheSameName.getTerminalSession());
    }

    @Test
    public void reconnectingABackgroundSessionThatDiedDoesNotChangeTheDisplayedSession() throws Exception {
        TermuxSession diedSession = connectedThenDiedSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(diedSession);
        TerminalSession displayedTerminalSession = terminalView.mTermSession;

        activity.getTermuxTerminalSessionClient().onSessionFinished(diedSession.getTerminalSession());
        idlePastTheReconnectPacer();

        assertSame("reconnecting a session that died in the background must leave the session the owner is"
                + " reading on screen, because switching it away loses the place he was reading",
            displayedTerminalSession, terminalView.mTermSession);
    }

    @Test
    public void aSessionWhoseProcessDiedWithoutEverProducingOutputIsLeftToTheBackgroundScan()
        throws Exception {
        TermuxSession neverConnectedSession = neverConnectedSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(neverConnectedSession);
        TerminalSession neverConnectedTerminalSession = neverConnectedSession.getTerminalSession();

        activity.getTermuxTerminalSessionClient().onSessionFinished(neverConnectedTerminalSession);
        idlePastTheReconnectPacer();

        TermuxSession sessionUnderTheSameName =
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull(sessionUnderTheSameName);
        assertSame("a shell process that dies without ever producing a byte never connected, so rebuilding"
                + " it the instant it dies spends a main-thread process spawn on a command that is failing"
                + " and would repeat for every session the app has just started",
            neverConnectedTerminalSession, sessionUnderTheSameName.getTerminalSession());
    }

    @Test
    public void aConnectedSessionThatDiesAgainImmediatelyIsLeftToTheBackgroundScanInsteadOfLooping()
        throws Exception {
        TermuxSession diedSession = connectedThenDiedSession(BACKGROUND_SESSION_NAME);
        shellManager.mTermuxSessions.add(diedSession);

        activity.getTermuxTerminalSessionClient().onSessionFinished(diedSession.getTerminalSession());
        idlePastTheReconnectPacer();

        TermuxSession firstReplacement = service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull(firstReplacement);
        TerminalSession firstReplacementTerminalSession = firstReplacement.getTerminalSession();
        markProcessDied(firstReplacementTerminalSession);
        markBytesProcessed(firstReplacementTerminalSession);
        activity.getSessionNewActivityStore().clearReconnecting(BACKGROUND_SESSION_NAME);

        activity.getTermuxTerminalSessionClient().onSessionFinished(firstReplacementTerminalSession);
        idlePastTheReconnectPacer();

        TermuxSession sessionUnderTheSameName =
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME);
        assertNotNull(sessionUnderTheSameName);
        assertSame("a command that connects and then fails again straight away would be rebuilt in a tight"
                + " loop if every death reconnected it, so the repeats must be spaced and the session left"
                + " to the background scan until the wait elapses",
            firstReplacementTerminalSession, sessionUnderTheSameName.getTerminalSession());
    }

    @Test
    public void aSessionAlreadyRemovedFromTheListIsNotBroughtBackByItsProcessDying() throws Exception {
        TermuxSession removedSession = connectedThenDiedSession(BACKGROUND_SESSION_NAME);

        activity.getTermuxTerminalSessionClient().onSessionFinished(removedSession.getTerminalSession());
        idlePastTheReconnectPacer();

        assertNull("a session the owner already removed is gone on purpose, so the death of its shell"
                + " process must not resurrect it under its old name",
            service.getTermuxSessionForSessionName(BACKGROUND_SESSION_NAME));
    }

    private void idlePastTheReconnectPacer() {
        ShadowLooper.idleMainLooper(
            SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS * 2, TimeUnit.MILLISECONDS);
    }

    private TermuxSession connectedSession(String name) throws Exception {
        return termuxSession(name, false, true);
    }

    private TermuxSession connectedThenDiedSession(String name) throws Exception {
        return termuxSession(name, true, true);
    }

    private TermuxSession neverConnectedSession(String name) throws Exception {
        return termuxSession(name, true, false);
    }

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

    private void markProcessRunning(TerminalSession terminalSession) throws Exception {
        shellPidField().setInt(terminalSession, RUNNING_SHELL_PROCESS_PID);
    }

    private void markProcessDied(TerminalSession terminalSession) throws Exception {
        shellPidField().setInt(terminalSession, -1);
    }

    private void markBytesProcessed(TerminalSession terminalSession) throws Exception {
        Field totalBytesProcessed = TerminalSession.class.getDeclaredField("mTotalBytesProcessed");
        totalBytesProcessed.setAccessible(true);
        totalBytesProcessed.setLong(terminalSession, BYTES_A_CONNECTED_SESSION_HAS_PROCESSED);
    }

    private Field shellPidField() throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        return shellPid;
    }

    private TermuxSession termuxSession(String name, boolean died, boolean everProducedOutput)
        throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        if (died) {
            markProcessDied(terminalSession);
        } else {
            markProcessRunning(terminalSession);
        }
        if (everProducedOutput) {
            markBytesProcessed(terminalSession);
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
