package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import com.termux.terminal.TerminalEmulator;
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
public class OpenedHiddenSessionUnhideTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String OPENED_HIDDEN_SESSION_NAME = "session-opened";

    private static final String UNOPENED_HIDDEN_SESSION_NAME = "session-untouched";

    private static final int RESIDENT_SHELL_PROCESS_PID = 1;

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TerminalSession currentTerminalSession;

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

        TermuxSession currentSession = sessionHoldingAnEmulatorAndAShellProcess(CURRENT_SESSION_NAME);
        shellManager.mTermuxSessions.add(currentSession);
        currentTerminalSession = currentSession.getTerminalSession();
        terminalView.mTermSession = currentTerminalSession;
    }

    @Test
    public void openingASessionThatIsMarkedHiddenLeavesItNotMarkedHidden() throws Exception {
        TerminalSession openedTerminalSession = addHiddenSession(OPENED_HIDDEN_SESSION_NAME);

        open(openedTerminalSession);

        assertFalse("opening a session that is marked hidden must clear its hidden mark, so it holds "
                + "its runtime resources like any other visible session",
            preferences.isSessionDisabled(OPENED_HIDDEN_SESSION_NAME));
    }

    @Test
    public void anOpenedHiddenSessionSurvivesAReclamationSweepAfterAnotherSessionBecomesCurrent()
        throws Exception {
        TerminalSession openedTerminalSession = addHiddenSession(OPENED_HIDDEN_SESSION_NAME);

        open(openedTerminalSession);
        open(currentTerminalSession);
        activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();

        assertNotNull("a session the owner opened must keep its terminal emulator and the scrollback "
                + "buffer it owns across a reclamation sweep tick, even once another session is "
                + "current", terminalSessionNamed(OPENED_HIDDEN_SESSION_NAME).getEmulator());
        assertTrue("a session the owner opened must keep the shell process it started across a "
                + "reclamation sweep tick, even once another session is current",
            terminalSessionNamed(OPENED_HIDDEN_SESSION_NAME).isRunning());
    }

    @Test
    public void aSessionThatIsMarkedHiddenAndWasNotOpenedIsStillReleasedByTheReclamationSweep()
        throws Exception {
        addHiddenSession(UNOPENED_HIDDEN_SESSION_NAME);

        activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();

        assertNull("a session that is still marked hidden must hold no terminal emulator and no "
                + "scrollback buffer after a reclamation sweep tick",
            terminalSessionNamed(UNOPENED_HIDDEN_SESSION_NAME).getEmulator());
        assertFalse("a session that is still marked hidden must hold no shell process after a "
            + "reclamation sweep tick", terminalSessionNamed(UNOPENED_HIDDEN_SESSION_NAME).isRunning());
    }

    private void open(TerminalSession terminalSession) {
        activity.getTermuxTerminalSessionClient().switchToSessionReconnectingIfDead(terminalSession);
    }

    private TerminalSession addHiddenSession(String sessionName) throws Exception {
        TermuxSession hiddenSession = sessionHoldingAnEmulatorAndAShellProcess(sessionName);
        shellManager.mTermuxSessions.add(hiddenSession);
        hide(sessionName);
        return hiddenSession.getTerminalSession();
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull("the row of a session must stay in the session list so the owner can reopen it",
            termuxSession);
        return termuxSession.getTerminalSession();
    }

    private void hide(String... sessionNames) {
        Set<String> hiddenSessionNames = new LinkedHashSet<>();
        for (String sessionName : sessionNames) {
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(hiddenSessionNames));
    }

    private TermuxSession sessionHoldingAnEmulatorAndAShellProcess(String sessionName)
        throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, RESIDENT_SHELL_PROCESS_PID);
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
        return termuxSession(terminalSession);
    }

    private static TermuxSession termuxSession(TerminalSession terminalSession) throws Exception {
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
