package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.PersistedSessionRestoreData;
import com.termux.app.terminal.session.PersistedSessionSerializer;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
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
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionReconnectSwitchGuardWiringTest {

    private static final String DISPLAYED_SESSION = "displayed-session";
    private static final String OTHER_LIVE_SESSION = "other-live-session";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TerminalView terminalView;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setAutosshCommand("ssh {name}");

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        shellManager.mTermuxSessions.add(liveSession(DISPLAYED_SESSION));
        shellManager.mTermuxSessions.add(liveSession(OTHER_LIVE_SESSION));
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController",
            new TermuxSessionsListViewController(activity, service.getTermuxSessions()));
        terminalView.mTermSession = terminalSession(DISPLAYED_SESSION);
    }

    @Test
    public void reconnectHelperKeepsTheAlreadyDisplayedSession() {
        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();

        assertEquals("the reconnect switch must only happen when no session is displayed",
            DISPLAYED_SESSION, currentSessionName());
    }

    @Test
    public void restorePersistedSessionsKeepsTheAlreadyDisplayedSession() throws Exception {
        preferences.setPersistedSessions(new PersistedSessionSerializer().serialize(
            Collections.singletonList(new PersistedSessionRestoreData(
                null, "restored-session", "/system/bin/sh", new String[0], false, "/"))));

        assertTrue(activity.getTermuxTerminalSessionClient().restorePersistedSessions());

        assertEquals("restoring sessions must not change the displayed session when one is already displayed",
            DISPLAYED_SESSION, currentSessionName());
    }

    @Test
    public void restoreAlwaysPresentSessionsKeepsTheAlreadyDisplayedSession() {
        assertTrue(activity.getTermuxTerminalSessionClient()
            .restoreAlwaysPresentSessions(Collections.singletonList("always-present-session")));

        assertEquals("creating always-present sessions must not change the displayed session when one is "
                + "already displayed",
            DISPLAYED_SESSION, currentSessionName());
    }

    @Test
    public void onStartKeepsTheAlreadyDisplayedSession() {
        activity.getTermuxTerminalSessionClient().onStart();

        assertEquals("returning to the foreground must never yank the user away from the session they "
                + "are working in",
            DISPLAYED_SESSION, currentSessionName());
    }

    @Test
    public void reconnectHelperSelectsASessionWhenNoneIsDisplayed() {
        terminalView.mTermSession = null;

        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();

        assertNotNull("with no session displayed the reconnect helper must select one",
            activity.getCurrentSession());
    }

    @Test
    public void reconnectHelperFallsBackToTheStoredSessionRatherThanTheLastOneWhenEverySessionIsHidden() {
        terminalView.mTermSession = null;
        preferences.setCurrentSession(terminalSession(DISPLAYED_SESSION).mHandle);
        preferences.setDisabledSessionNames(String.join("\n", DISPLAYED_SESSION, OTHER_LIVE_SESSION));

        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();

        assertEquals("with no non-hidden session left, the fallback must still be the stored session in "
                + "preference to the last running one",
            DISPLAYED_SESSION, currentSessionName());
    }

    private String currentSessionName() {
        TerminalSession currentSession = activity.getCurrentSession();
        assertNotNull(currentSession);
        return currentSession.mSessionName;
    }

    private TerminalSession terminalSession(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession();
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
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
