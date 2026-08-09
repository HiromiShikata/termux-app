package com.termux.app.terminal;

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

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
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BackgroundReconnectTargetsDroppedConnectionsOnlyTest {

    private static final String DISPLAYED_SESSION_NAME = "session-displayed";

    private static final String DROPPED_SESSION_NAME = "session-dropped";

    private static final String QUIET_SESSION_NAME = "session-quiet";

    private static final int DEAD_SHELL_PROCESS_ID = -1;

    private static final int RUNNING_SHELL_PROCESS_ID = 4242;

    private static final long ANCIENT_CALL_TIME_MILLIS = 1_000L;

    private static final long ANCIENT_OUT_TIME_MILLIS = 1_500L;

    private static final long ANSWERED_REPLY_TIME_MILLIS = 2_000L;

    private static final long DRAIN_HORIZON_MILLIS = 20_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final int DRAIN_ATTEMPT_LIMIT = 2_000;

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private final Set<TerminalSession> sessionsPresentBeforeTheSweep = new LinkedHashSet<>();

    private long sweepStartUptimeMillis;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        sessionActivityClient = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", sessionActivityClient);
        set(activity, TermuxActivity.class, "mIsVisible", true);
        service.setTermuxTerminalSessionClient(sessionActivityClient);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TerminalSession displayedSession = addRunningSession(DISPLAYED_SESSION_NAME);
        terminalView.mTermSession = displayedSession;

        addDeadSession(DROPPED_SESSION_NAME);
        addRunningSession(QUIET_SESSION_NAME);
        recordASessionThatHasBeenQuietForALongTimeWithNoCallOutstanding(QUIET_SESSION_NAME);

        for (TermuxSession termuxSession : shellManager.mTermuxSessions) {
            sessionsPresentBeforeTheSweep.add(termuxSession.getTerminalSession());
        }
        idleMainThreadTasksDueNow();
    }

    @Test
    public void aSessionWhoseShellProcessIsGoneIsRecoveredByTheBackgroundScan() {
        runBackgroundReconnectSweep();
        drainMainThreadQueueToTheSweepHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        assertTrue("a session whose shell process has exited has really lost its connection, and the "
                + "owner is left with a session that shows nothing and accepts nothing until it is "
                + "replaced, so the background scan must recover it; the sessions the scan recovered "
                + "were " + reconnectedSessionNames,
            reconnectedSessionNames.contains(DROPPED_SESSION_NAME));
    }

    @Test
    public void aQuietSessionWhoseConnectionIsHealthyIsLeftAlone() {
        runBackgroundReconnectSweep();
        drainMainThreadQueueToTheSweepHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        assertTrue("producing no output is not a lost connection: a session an agent left idle waiting "
                + "for the owner is silent for as long as the owner takes to answer, and the app can "
                + "tell its connection is healthy from the live shell process and from input still "
                + "reaching the program reading the terminal; replacing it anyway tears down a working "
                + "connection, discards input the owner already gave it and loses the call it was "
                + "waiting on, and the session named " + QUIET_SESSION_NAME + " has a live shell process "
                + "and is only quiet, yet the sessions the scan replaced were " + reconnectedSessionNames,
            !reconnectedSessionNames.contains(QUIET_SESSION_NAME));
    }

    private void recordASessionThatHasBeenQuietForALongTimeWithNoCallOutstanding(String sessionName) {
        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        store.recordStatuslineTimes(sessionName, ANCIENT_CALL_TIME_MILLIS, ANCIENT_OUT_TIME_MILLIS,
            ANSWERED_REPLY_TIME_MILLIS);
        assertTrue("the quiet session must carry no unanswered call, otherwise the sweep would "
                + "materialize it for a reason this test is not about",
            store.pendingCallToUserSinceTimeMillis(sessionName) == null);
    }

    private void runBackgroundReconnectSweep() {
        sweepStartUptimeMillis = SystemClock.uptimeMillis();
        try {
            sessionActivityClient.startDisplayedSessionCallScanTick();
        } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
            assertItIsTheNativeSubprocessLibrary(nativeSubprocessUnavailableOffDevice);
        }
        sessionActivityClient.stopDisplayedSessionCallScanTick();
    }

    private void drainMainThreadQueueToTheSweepHorizon() {
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis - sweepStartUptimeMillis > DRAIN_HORIZON_MILLIS) return;
            advanceMainThreadClockTo(nextTaskDueUptimeMillis);
        }
    }

    private void advanceMainThreadClockTo(long targetUptimeMillis) {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_LIMIT; attempt++) {
            long remainingMillis = Math.max(0L, targetUptimeMillis - SystemClock.uptimeMillis());
            try {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(remainingMillis));
                return;
            } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
                assertItIsTheNativeSubprocessLibrary(nativeSubprocessUnavailableOffDevice);
            }
        }
    }

    private void idleMainThreadTasksDueNow() {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_LIMIT; attempt++) {
            try {
                shadowOf(Looper.getMainLooper()).idle();
                return;
            } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
                assertItIsTheNativeSubprocessLibrary(nativeSubprocessUnavailableOffDevice);
            }
        }
    }

    private void assertItIsTheNativeSubprocessLibrary(LinkageError linkageError) {
        StringBuilder causeChain = new StringBuilder();
        Throwable current = linkageError;
        while (current != null) {
            causeChain.append(current).append('\n');
            for (StackTraceElement element : current.getStackTrace()) {
                causeChain.append("    at ").append(element).append('\n');
            }
            current = current.getCause();
        }
        assertTrue("the only linkage failure this test tolerates is the device-only native subprocess "
                + "library, and anything else means the run failed for a reason the assertions would "
                + "then hide:\n" + causeChain,
            causeChain.toString().contains("com.termux.terminal.JNI"));
    }

    private Set<String> reconnectedSessionNames() {
        Set<String> reconnectedSessionNames = new LinkedHashSet<>();
        for (TermuxSession termuxSession : new ArrayList<TermuxSession>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionsPresentBeforeTheSweep.contains(terminalSession)) continue;
            if (terminalSession.mSessionName == null) continue;
            reconnectedSessionNames.add(terminalSession.mSessionName);
        }
        return reconnectedSessionNames;
    }

    private TerminalSession addDeadSession(String sessionName) throws Exception {
        TerminalSession terminalSession = addSession(sessionName);
        set(terminalSession, TerminalSession.class, "mShellPid", DEAD_SHELL_PROCESS_ID);
        return terminalSession;
    }

    private TerminalSession addRunningSession(String sessionName) throws Exception {
        TerminalSession terminalSession = addSession(sessionName);
        set(terminalSession, TerminalSession.class, "mShellPid", RUNNING_SHELL_PROCESS_ID);
        return terminalSession;
    }

    private TerminalSession addSession(String sessionName) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 2000,
                sessionActivityClient);
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        return terminalSession;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
