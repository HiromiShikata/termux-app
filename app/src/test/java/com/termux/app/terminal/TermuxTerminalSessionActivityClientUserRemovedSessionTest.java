package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
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

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionActivityClientUserRemovedSessionTest {

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
