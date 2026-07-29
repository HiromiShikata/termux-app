package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class DeadSessionRetentionAcrossBackgroundScansTest {

    private static final class SessionListTreatingEveryProjectRowAsCollapsed
            extends TermuxSessionsListViewController {

        SessionListTreatingEveryProjectRowAsCollapsed(@NonNull TermuxActivity activity) {
            super(activity, new ArrayList<>());
        }

        @NonNull
        @Override
        public Set<String> getExpandedProjectSessionNames() {
            return Collections.emptySet();
        }
    }

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final int VISIBLE_SESSION_COUNT = 16;

    private static final int OUT_OF_SIGHT_SESSION_COUNT = 15;

    private static final int DEAD_OUT_OF_SIGHT_SESSION_COUNT = 14;

    private static final int BACKGROUND_SCAN_CYCLE_COUNT = 3;

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

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

        set(activity, TermuxActivity.class, "mTermuxSessionListViewController",
            new SessionListTreatingEveryProjectRowAsCollapsed(activity));

        TermuxSession currentSession = liveSession(CURRENT_SESSION_NAME);
        shellManager.mTermuxSessions.add(currentSession);
        terminalView.mTermSession = currentSession.getTerminalSession();
    }

    @Test
    public void deadOutOfSightSessionsMustNotKeepTheirTerminalEmulatorAcrossRepeatedScanCycles()
        throws Exception {
        seedRealisticSessionPopulation();

        assertEquals("the seeded population must match the reported device state before any scan runs",
            VISIBLE_SESSION_COUNT + OUT_OF_SIGHT_SESSION_COUNT, service.getTermuxSessionsSize());
        assertEquals("every seeded dead out of sight session must start out holding a terminal emulator and "
                + "its scrollback buffer, otherwise this test would not be measuring the retention at all",
            DEAD_OUT_OF_SIGHT_SESSION_COUNT, deadOutOfSightSessionsHoldingAnEmulator());

        for (int scanCycle = 0; scanCycle < BACKGROUND_SCAN_CYCLE_COUNT; scanCycle++) {
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();
            activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        }

        assertEquals("the rows of dead sessions stay in the list on purpose so the owner can still see "
                + "and reopen them, so the scan cycles must not remove them",
            VISIBLE_SESSION_COUNT + OUT_OF_SIGHT_SESSION_COUNT, service.getTermuxSessionsSize());
        assertEquals("a session whose process has exited and which is neither current nor displayed must "
                + "release its terminal emulator and its scrollback buffer, whose size is governed by the "
                + "transcript rows setting, instead of holding them for the lifetime of the process",
            0, deadOutOfSightSessionsHoldingAnEmulator());
    }

    @Test
    public void aDeadOutOfSightSessionMustReleaseItsEmulatorWhileItsRowStaysInTheList() throws Exception {
        String outOfSightDeadSessionName = "session-out-of-sight-alpha";
        TermuxSession outOfSightDeadSession = deadSessionHoldingAnEmulator(outOfSightDeadSessionName);
        shellManager.mTermuxSessions.add(outOfSightDeadSession);

        assertNotNull("the dead out of sight session must hold a terminal emulator before the scan runs",
            outOfSightDeadSession.getTerminalSession().getEmulator());

        for (int scanCycle = 0; scanCycle < BACKGROUND_SCAN_CYCLE_COUNT; scanCycle++) {
            activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();
            activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        }

        assertNotNull("the row of the dead out of sight session must stay in the list so the owner can still "
                + "see it and reopen it without any manual action",
            service.getTermuxSessionForSessionName(outOfSightDeadSessionName));
        assertNull("a session that has died out of sight is reconnected by no scan and rendered for nobody, so "
                + "the scan must release its terminal emulator and scrollback buffer while keeping its row",
            service.getTermuxSessionForSessionName(outOfSightDeadSessionName)
                .getTerminalSession().getEmulator());
    }

    private int deadOutOfSightSessionsHoldingAnEmulator() {
        int sessionsHoldingAnEmulator = 0;
        for (int outOfSightIndex = 1; outOfSightIndex <= DEAD_OUT_OF_SIGHT_SESSION_COUNT; outOfSightIndex++) {
            TermuxSession termuxSession =
                service.getTermuxSessionForSessionName(outOfSightSessionName(outOfSightIndex));
            if (termuxSession == null || termuxSession.getTerminalSession() == null) {
                continue;
            }
            if (termuxSession.getTerminalSession().getEmulator() != null) {
                sessionsHoldingAnEmulator++;
            }
        }
        return sessionsHoldingAnEmulator;
    }

    private void seedRealisticSessionPopulation() throws Exception {
        for (int visibleIndex = 1; visibleIndex < VISIBLE_SESSION_COUNT; visibleIndex++) {
            shellManager.mTermuxSessions.add(liveSession(visibleSessionName(visibleIndex)));
        }
        for (int outOfSightIndex = 1; outOfSightIndex <= OUT_OF_SIGHT_SESSION_COUNT; outOfSightIndex++) {
            String outOfSightSessionName = outOfSightSessionName(outOfSightIndex);
            shellManager.mTermuxSessions.add(outOfSightIndex <= DEAD_OUT_OF_SIGHT_SESSION_COUNT
                ? deadSessionHoldingAnEmulator(outOfSightSessionName)
                : liveSession(outOfSightSessionName));
        }
    }

    private static String visibleSessionName(int index) {
        return String.format("session-visible-%02d", index);
    }

    private static String outOfSightSessionName(int index) {
        return String.format("session-out-of-sight-%02d", index);
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        return termuxSession(terminalSession(sessionName));
    }

    private TermuxSession deadSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
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
