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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalSessionActivityClientRestoreSessionCapTest {

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private final PersistedSessionSerializer persistedSessionSerializer = new PersistedSessionSerializer();

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

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession alreadyDisplayedSession = liveSession("already-displayed");
        shellManager.mTermuxSessions.add(alreadyDisplayedSession);
        terminalView.mTermSession = alreadyDisplayedSession.getTerminalSession();
    }

    @Test
    public void restorePersistedSessionsRestoresSessionDespiteDeadOrphanOccupyingRawSlot() throws Exception {
        preferences.setSessionDefinitionMaxSessions(2);
        shellManager.mTermuxSessions.add(deadSession("dead-orphan"));

        List<PersistedSessionRestoreData> persistedSessions = Collections.singletonList(
            new PersistedSessionRestoreData(null, "persisted-target", "/system/bin/sh",
                new String[0], false, "/"));
        preferences.setPersistedSessions(persistedSessionSerializer.serialize(persistedSessions));

        assertEquals("raw session count already occupies the configured cap even though one slot is dead",
            2, service.getTermuxSessionsSize());

        boolean restored = activity.getTermuxTerminalSessionClient().restorePersistedSessions();

        assertTrue("a persisted session must still be restored when only a dead orphaned session, " +
            "not a live one, occupies the remaining raw slot", restored);
        assertNotNull("the persisted session must become a live tracked session",
            service.getTermuxSessionForSessionName("persisted-target"));
    }

    @Test
    public void restoreAlwaysPresentSessionsCreatesSessionDespiteDeadOrphanOccupyingRawSlot() throws Exception {
        preferences.setSessionDefinitionMaxSessions(2);
        shellManager.mTermuxSessions.add(deadSession("dead-orphan"));
        preferences.setAlwaysNaSessionNames("secretary");

        assertEquals("raw session count already occupies the configured cap even though one slot is dead",
            2, service.getTermuxSessionsSize());

        boolean created = activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions();

        assertTrue("the always-present session must still be created when only a dead orphaned session, " +
            "not a live one, occupies the remaining raw slot", created);
        assertNotNull("the always-present session must become a live tracked session",
            service.getTermuxSessionForSessionName("secretary"));
    }

    private TermuxSession liveSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private TermuxSession deadSession(String name) throws Exception {
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
