package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import android.content.Context;
import android.os.Looper;

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
import org.robolectric.Shadows;

import java.time.Duration;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
public class NonTerminatingHungSessionReconnectSelectionTest {

    private static final long PACED_RECONNECT_DRAIN_MILLIS = 1000L;

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String RECONNECTED_SESSION_NAME = "session-alpha";

    private static final long ELEVEN_MINUTES_MILLIS = 11L * 60L * 1000L;

    private static final int SUBSEQUENT_SCAN_COUNT = 2;

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
        preferences.setSessionDefinitionMaxSessions(16);

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
    public void aReplacementSessionThatNeverReceivedAnEmulatorMustNotBeReconnectedAgainOnEveryScan()
        throws Exception {
        shellManager.mTermuxSessions.add(deadSession(RECONNECTED_SESSION_NAME));
        activity.getSessionNewActivityStore().recordStatuslineTimes(RECONNECTED_SESSION_NAME, null,
            System.currentTimeMillis() - ELEVEN_MINUTES_MILLIS, null);

        TerminalSession sessionBeforeFirstScan = terminalSessionNamed(RECONNECTED_SESSION_NAME);

        runOneBackgroundScanCycle();
        TerminalSession sessionAfterFirstScan = terminalSessionNamed(RECONNECTED_SESSION_NAME);
        assertNotSame("the first scan must reconnect the dead session, otherwise the following "
                + "assertions would not be exercising a replacement session at all",
            sessionBeforeFirstScan, sessionAfterFirstScan);
        assertEquals("the replacement session must not have become the current session, otherwise the "
                + "reconnect planner would skip it for a reason unrelated to this defect",
            CURRENT_SESSION_NAME, currentSessionName());

        TerminalSession sessionBeforeThisScan = sessionAfterFirstScan;
        int reselectionCount = 0;
        for (int subsequentScan = 0; subsequentScan < SUBSEQUENT_SCAN_COUNT; subsequentScan++) {
            runOneBackgroundScanCycle();
            TerminalSession sessionAfterThisScan = terminalSessionNamed(RECONNECTED_SESSION_NAME);
            if (sessionAfterThisScan != sessionBeforeThisScan) {
                reselectionCount++;
            }
            sessionBeforeThisScan = sessionAfterThisScan;
        }

        assertEquals("a replacement session that never received an emulator reports itself as running "
                + "and inherits the stale statusline output time of the session it replaced, so every "
                + "subsequent scan selects it again and tears it down; the reconnect selection has no "
                + "reachable exit, counted over " + SUBSEQUENT_SCAN_COUNT + " scans after the first",
            0, reselectionCount);
    }

    private void runOneBackgroundScanCycle() {
        activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        Shadows.shadowOf(Looper.getMainLooper())
            .idleFor(Duration.ofMillis(PACED_RECONNECT_DRAIN_MILLIS));
    }

    private String currentSessionName() {
        TerminalSession currentSession = activity.getCurrentSession();
        return currentSession == null ? null : currentSession.mSessionName;
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull("the session must still be tracked by the service", termuxSession);
        return termuxSession.getTerminalSession();
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
