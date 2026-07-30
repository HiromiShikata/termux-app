package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionNameReachesNoSessionCreationCallTest {

    private static final String HIDDEN_SESSION_NAME = "hidden-agent";
    private static final String SHOWN_SESSION_NAME = "shown-agent";
    private static final String DISPLAYED_SESSION_NAME = "displayed-agent";
    private static final int LIVE_SHELL_PROCESS_ID = 4242;
    private static final int SESSION_CAP = 16;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private CreationCallRecordingSessionClient recordingSessionClient;

    private static final class CreationCallRecordingSessionClient
            extends TermuxTerminalSessionActivityClient {

        private final List<String> sessionNamesTheCreationCallWasReachedFor = new ArrayList<>();

        private CreationCallRecordingSessionClient(TermuxActivity activity) {
            super(activity);
        }

        @Override
        public void addNewSession(boolean isFailSafe, String sessionName, boolean closeDrawerAfter) {
            sessionNamesTheCreationCallWasReachedFor.add(sessionName);
        }

        @Override
        public void addNewAutosshSession(String sessionName, String command, boolean closeDrawerAfter) {
            sessionNamesTheCreationCallWasReachedFor.add(sessionName);
        }

        private List<String> sessionNamesTheCreationCallWasReachedFor() {
            return sessionNamesTheCreationCallWasReachedFor;
        }
    }

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        recordingSessionClient = new CreationCallRecordingSessionClient(activity);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", recordingSessionClient);
        service.setTermuxTerminalSessionClient(recordingSessionClient);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(Collections.emptyList());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(SESSION_CAP);
        preferences.setAlwaysNaSessionNames(
            DISPLAYED_SESSION_NAME + "\n" + HIDDEN_SESSION_NAME + "\n" + SHOWN_SESSION_NAME);
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            new LinkedHashSet<>(Collections.singletonList(HIDDEN_SESSION_NAME))));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();
    }

    @Test
    public void aDefinitionLoadWithAnEmptyEntryListNeverReachesTheSessionCreationCallForAHiddenName()
            throws Exception {
        assertTrue("the arrangement must record the name as hidden before the assertions below mean "
                + "anything; the stored hidden set was " + preferences.getDisabledSessionNames(),
            preferences.getDisabledSessionNames().contains(HIDDEN_SESSION_NAME));

        loadTheSessionDefinitionWithAnEmptyEntryList();

        List<String> sessionNamesTheCreationCallWasReachedFor =
            recordingSessionClient.sessionNamesTheCreationCallWasReachedFor();
        assertEquals("drawing a row for a hidden name must never turn into a session, so the creation "
                + "call must be reached for exactly the always-present name the owner did not hide; the "
                + "creation call was reached for " + sessionNamesTheCreationCallWasReachedFor,
            Collections.singletonList(SHOWN_SESSION_NAME), sessionNamesTheCreationCallWasReachedFor);
        assertFalse("the session creation call must never be reached for a name the owner has hidden, "
                + "because a hidden session is required to hold no shell process, no terminal emulator "
                + "and no session cap slot until the owner opens it himself; the creation call was "
                + "reached for " + sessionNamesTheCreationCallWasReachedFor,
            sessionNamesTheCreationCallWasReachedFor.contains(HIDDEN_SESSION_NAME));
    }

    private void loadTheSessionDefinitionWithAnEmptyEntryList() throws Exception {
        SessionDefinitionLoadResult emptyEntryListResult =
            new SessionDefinitionLoadResult(Collections.emptyList(), 1, Collections.emptyList());
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method buildSessions = SessionDefinitionController.class
            .getDeclaredMethod("buildSessions", SessionDefinitionLoadResult.class);
        buildSessions.setAccessible(true);
        buildSessions.invoke(controller, emptyEntryListResult);
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, LIVE_SHELL_PROCESS_ID);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
