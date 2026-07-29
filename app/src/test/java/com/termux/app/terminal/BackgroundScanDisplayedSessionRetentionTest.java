package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;

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

@RunWith(RobolectricTestRunner.class)
public class BackgroundScanDisplayedSessionRetentionTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String DISPLAYED_DEAD_SESSION_NAME = "session-displayed-dead";

    private static final int BACKGROUND_SCAN_CYCLE_COUNT = 3;

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

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
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession currentSession = termuxSession(terminalSession(CURRENT_SESSION_NAME));
        shellManager.mTermuxSessions.add(currentSession);
        terminalView.mTermSession = currentSession.getTerminalSession();
    }

    @Test
    public void aDisplayedDeadSessionMustKeepItsEmulatorWhenTheScanRunsWhileTheActivityIsNotVisible()
        throws Exception {
        set(activity, TermuxActivity.class, "mIsVisible", false);
        TermuxSession displayedDeadSession =
            termuxSession(deadSessionHoldingAnEmulator(DISPLAYED_DEAD_SESSION_NAME));
        shellManager.mTermuxSessions.add(displayedDeadSession);

        for (int scanCycle = 0; scanCycle < BACKGROUND_SCAN_CYCLE_COUNT; scanCycle++) {
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();
        }

        assertNotNull("a session that is a row of the on-screen session list is not undisplayed merely "
                + "because the activity is in the background, so a scan reached while the activity is "
                + "not visible must not release its terminal emulator and scrollback buffer and force "
                + "the next return to the foreground to rebuild every row at once",
            service.getTermuxSessionForSessionName(DISPLAYED_DEAD_SESSION_NAME)
                .getTerminalSession().getEmulator());
    }

    private TerminalSession deadSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        set(terminalSession, TerminalSession.class, "mEmulator",
            new TerminalEmulator(terminalSession, TERMINAL_COLUMNS, TERMINAL_ROWS,
                TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
                activity.getTermuxTerminalSessionClient()));
        return terminalSession;
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
