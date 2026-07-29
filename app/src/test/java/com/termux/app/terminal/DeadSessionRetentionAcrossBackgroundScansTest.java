package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
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
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class DeadSessionRetentionAcrossBackgroundScansTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final int VISIBLE_SESSION_COUNT = 16;

    private static final int HIDDEN_SESSION_COUNT = 15;

    private static final int DEAD_HIDDEN_SESSION_COUNT = 14;

    private static final int BACKGROUND_SCAN_CYCLE_COUNT = 3;

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
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession currentSession = liveSession(CURRENT_SESSION_NAME);
        shellManager.mTermuxSessions.add(currentSession);
        terminalView.mTermSession = currentSession.getTerminalSession();
    }

    @Test
    public void deadHiddenSessionsMustNotSurviveRepeatedBackgroundScanCycles() throws Exception {
        seedRealisticSessionPopulation();

        assertEquals("the seeded population must match the reported device state before any scan runs",
            VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT, service.getTermuxSessionsSize());

        for (int scanCycle = 0; scanCycle < BACKGROUND_SCAN_CYCLE_COUNT; scanCycle++) {
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();
            activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        }

        assertEquals("dead sessions that are neither displayed nor touched by the owner must be "
                + "reclaimed by the background scan instead of being retained with their terminal "
                + "emulator and scrollback buffer for the lifetime of the process",
            VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT - DEAD_HIDDEN_SESSION_COUNT,
            service.getTermuxSessionsSize());
    }

    @Test
    public void aSingleHiddenSessionThatHasDiedMustBeRemovedByAScan() throws Exception {
        String hiddenDeadSessionName = "session-hidden-alpha";
        shellManager.mTermuxSessions.add(deadSession(hiddenDeadSessionName));
        hideSessionNames(hiddenDeadSessionName);

        assertNotNull("the hidden dead session must be present before the scan runs",
            service.getTermuxSessionForSessionName(hiddenDeadSessionName));

        for (int scanCycle = 0; scanCycle < BACKGROUND_SCAN_CYCLE_COUNT; scanCycle++) {
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();
            activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        }

        assertNull("a hidden session that has died is reconnected by no scan and displayed to nobody, "
                + "so no scan may leave it in the service session list",
            service.getTermuxSessionForSessionName(hiddenDeadSessionName));
    }

    private void seedRealisticSessionPopulation() throws Exception {
        for (int visibleIndex = 1; visibleIndex < VISIBLE_SESSION_COUNT; visibleIndex++) {
            shellManager.mTermuxSessions.add(liveSession(visibleSessionName(visibleIndex)));
        }
        Set<String> hiddenSessionNames = new LinkedHashSet<>();
        for (int hiddenIndex = 1; hiddenIndex <= HIDDEN_SESSION_COUNT; hiddenIndex++) {
            String hiddenSessionName = hiddenSessionName(hiddenIndex);
            hiddenSessionNames.add(hiddenSessionName);
            shellManager.mTermuxSessions.add(hiddenIndex <= DEAD_HIDDEN_SESSION_COUNT
                ? deadSession(hiddenSessionName)
                : liveSession(hiddenSessionName));
        }
        hideSessionNames(hiddenSessionNames.toArray(new String[0]));
    }

    private void hideSessionNames(String... sessionNames) {
        Set<String> hiddenSessionNames = new LinkedHashSet<>();
        for (String sessionName : sessionNames) {
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(hiddenSessionNames));
    }

    private static String visibleSessionName(int index) {
        return String.format("session-visible-%02d", index);
    }

    private static String hiddenSessionName(int index) {
        return String.format("session-hidden-%02d", index);
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        return termuxSession(terminalSession(sessionName));
    }

    private TermuxSession deadSession(String sessionName) throws Exception {
        TerminalSession terminalSession = terminalSession(sessionName);
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        return termuxSession(terminalSession);
    }

    private static TerminalSession terminalSession(String sessionName) {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        return terminalSession;
    }

    private static TermuxSession termuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
