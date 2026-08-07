package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
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
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class DisplayedSessionReconnectsItselfTest {

    private static final String DISPLAYED_SESSION_NAME = "session-displayed";

    private static final int OTHER_VISIBLE_SESSION_COUNT = 15;

    private static final int HIDDEN_SESSION_COUNT = 4;

    private static final int SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS = 1;

    private static final long DRAIN_HORIZON_MILLIS_BEFORE_THE_FIRST_RECONNECT_TIMEOUT = 20_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final int DRAIN_ATTEMPT_LIMIT = 2_000;

    private static final int DEAD_SHELL_PROCESS_ID = -1;

    private static final int RUNNING_SHELL_PROCESS_ID = 4242;

    private static final long SILENT_FOR_LONGER_THAN_THE_RECONNECT_STALENESS_THRESHOLD_MILLIS =
        10L * 60L * 1000L;

    private TermuxActivity activity;

    private TermuxService service;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private TerminalView terminalView;

    private TerminalSession displayedSession;

    private TermuxAppSharedPreferences preferences;

    private final List<TerminalSession> otherVisibleSessions = new ArrayList<>();

    private final List<TerminalSession> hiddenSessions = new ArrayList<>();

    private final Set<String> hiddenSessionNames = new LinkedHashSet<>();

    private final Set<TerminalSession> sessionsPresentBeforeTheScan = new LinkedHashSet<>();

    private final Set<String> sessionNamesPresentBeforeTheScan = new LinkedHashSet<>();

    private final List<Throwable> throwablesSurfacedByTheScan = new ArrayList<>();

    private long scanStartUptimeMillis;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        sessionActivityClient = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", sessionActivityClient);
        set(activity, TermuxActivity.class, "mIsVisible", true);
        service.setTermuxTerminalSessionClient(sessionActivityClient);

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        displayedSession = addDeadSession(DISPLAYED_SESSION_NAME);
        terminalView.mTermSession = displayedSession;

        for (int index = 1; index <= OTHER_VISIBLE_SESSION_COUNT; index++) {
            otherVisibleSessions.add(addDeadSession(String.format("session-visible-%02d", index)));
        }
        for (int index = 1; index <= HIDDEN_SESSION_COUNT; index++) {
            String sessionName = String.format("session-hidden-%02d", index);
            hiddenSessions.add(addDeadSession(sessionName));
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(String.join("\n", hiddenSessionNames));

        recordThePopulationPresentBeforeTheScan();
        idleMainThreadTasksDueNow();
    }

    @Test
    public void theDisplayedSessionScanReconnectsTheDisplayedSessionWhenItIsDead() {
        runDisplayedSessionScan();
        drainMainThreadQueueToTheScanHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        List<String> otherVisibleSessionNamesNotReconnected =
            otherVisibleSessionNamesNotIn(reconnectedSessionNames);

        assertTrue("the owner runs sixteen visible sessions on a phone that loses connectivity "
                + "routinely, and the displayed-session scan is one of the two paths that can revive a "
                + "dead session with no owner input; it collects the displayed session's name and must "
                + "reconnect it like any other dead visible session, otherwise the one session the owner "
                + "is actually looking at is the only one he has to tap by hand after every outage; the "
                + "session named " + DISPLAYED_SESSION_NAME + " was dead when the scan ran, yet the "
                + "sessions the scan reconnected were " + reconnectedSessionNames,
            reconnectedSessionNames.contains(DISPLAYED_SESSION_NAME));

        assertTrue("reconnecting the displayed session must be added to the work the scan already does, "
                + "never traded against it: a change that made room for the displayed session by dropping "
                + "the other dead visible sessions from the candidate set would satisfy the assertion "
                + "above while leaving the owner worse off than before; all "
                + OTHER_VISIBLE_SESSION_COUNT + " other dead visible sessions must still be reconnected "
                + "in the same pass, yet these were not: " + otherVisibleSessionNamesNotReconnected,
            otherVisibleSessionNamesNotReconnected.isEmpty());
    }

    @Test
    public void theBackgroundReconnectScanReconnectsTheDisplayedSessionWhenItIsDead() {
        List<String> sessionNamesTheBackgroundScanSelected = runBackgroundReconnectScan();
        drainMainThreadQueueToTheScanHorizon();

        assertTrue("with the session list closed the background reconnect scan's name pool is the "
                + "displayed session's name alone, so the displayed session is the only session that scan "
                + "can ever revive and dropping it leaves the scan reconnecting nothing at all on every "
                + "one of its five-minute ticks; the session named " + DISPLAYED_SESSION_NAME + " was "
                + "dead when the scan ran, yet the names it selected for reconnect were "
                + sessionNamesTheBackgroundScanSelected,
            sessionNamesTheBackgroundScanSelected.contains(DISPLAYED_SESSION_NAME));

        TerminalSession sessionNamedDisplayedAfterTheScan = liveSessionNamed(DISPLAYED_SESSION_NAME);
        Set<String> liveSessionNamesAfterTheScan = liveSessionNames();
        List<String> shownSessionNamesLostByTheScan = new ArrayList<>();
        for (String sessionNamePresentBefore : sessionNamesPresentBeforeTheScan) {
            if (hiddenSessionNames.contains(sessionNamePresentBefore)) continue;
            if (!liveSessionNamesAfterTheScan.contains(sessionNamePresentBefore)) {
                shownSessionNamesLostByTheScan.add(sessionNamePresentBefore);
            }
        }

        assertNotNull("selecting the displayed session for reconnect is worthless unless a working "
                + "replacement really arrives: the reconnect removes the dead session before it creates "
                + "its replacement, so a change that selected the displayed session and then failed to "
                + "create anything would leave the owner staring at a row that vanished instead of one "
                + "that came back; a live session named " + DISPLAYED_SESSION_NAME + " must exist once "
                + "the main-thread queue drains",
            sessionNamedDisplayedAfterTheScan);
        assertNotSame("the replacement must be a genuinely new session rather than the same dead object "
                + "left in the list, otherwise nothing was reconnected at all",
            displayedSession, sessionNamedDisplayedAfterTheScan);
        assertTrue("no shown session may disappear from the live session list as a side effect of "
                + "reconnecting the displayed one, because a reconnect that removes more rows than it "
                + "recreates is doing less work rather than the right work; hidden sessions are left out "
                + "of this count because the same pass deliberately drops them from the live list to hold "
                + "no runtime resource at all; these shown names were present before the scan and gone "
                + "after it: " + shownSessionNamesLostByTheScan,
            shownSessionNamesLostByTheScan.isEmpty());
    }

    @Test
    public void theDisplayedSessionScanLeavesTheDisplayedSessionAloneWhileItIsRunningButSilent()
            throws Exception {
        makeTheDisplayedSessionRunningButSilent();

        runDisplayedSessionScan();
        drainMainThreadQueueToTheScanHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        List<String> otherVisibleSessionNamesNotReconnected =
            otherVisibleSessionNamesNotIn(reconnectedSessionNames);

        assertTrue("a displayed session that is still running but has gone silent may be one the owner "
                + "is typing into right now, and there is no in-place replacement for it, so tearing it "
                + "down would destroy its screen state and the input already in flight; only a dead "
                + "displayed session has nothing to lose; the session named " + DISPLAYED_SESSION_NAME
                + " was running and silent for "
                + SILENT_FOR_LONGER_THAN_THE_RECONNECT_STALENESS_THRESHOLD_MILLIS
                + " milliseconds when the scan ran, yet the sessions the scan reconnected were "
                + reconnectedSessionNames,
            !reconnectedSessionNames.contains(DISPLAYED_SESSION_NAME));
        assertSame("the terminal view must still be attached to the very session object the owner was "
                + "typing into, not to a replacement created behind his back",
            displayedSession, terminalView.mTermSession);
        assertTrue("this protection is worthless if the scan simply did nothing at all, so the same run "
                + "must show the scan really did the work it was asked to do: all "
                + OTHER_VISIBLE_SESSION_COUNT + " dead visible sessions must still be reconnected, yet "
                + "these were not: " + otherVisibleSessionNamesNotReconnected,
            otherVisibleSessionNamesNotReconnected.isEmpty());
    }

    @Test
    public void theDisplayedSessionScanConnectsNoHiddenSessionEvenWhenTheHiddenOneIsTheDisplayedSession() {
        hideSessionTheWayTheHideControlDoes(DISPLAYED_SESSION_NAME);

        runDisplayedSessionScan();
        drainMainThreadQueueToTheScanHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        List<String> hiddenSessionNamesConnected = new ArrayList<>();
        for (TerminalSession hiddenSession : hiddenSessions) {
            if (reconnectedSessionNames.contains(hiddenSession.mSessionName)) {
                hiddenSessionNamesConnected.add(hiddenSession.mSessionName);
            }
        }
        List<String> otherVisibleSessionNamesNotReconnected =
            otherVisibleSessionNamesNotIn(reconnectedSessionNames);

        assertTrue("hiding a session means it holds no runtime resource at all, and the selector that "
                + "feeds the displayed-session scan adds the current session's name without ever "
                + "filtering hidden names, so once the displayed session stops being excluded from the "
                + "reconnect candidates the hidden guards at the points of effect are the only thing left "
                + "preventing a hidden session the owner happens to be displaying from being woken up; "
                + "the session named " + DISPLAYED_SESSION_NAME + " was hidden through the same state "
                + "change the hide control writes, yet the sessions the scan reconnected were "
                + reconnectedSessionNames,
            !reconnectedSessionNames.contains(DISPLAYED_SESSION_NAME));
        assertTrue("no hidden session may be connected by any automatic path, yet these hidden sessions "
                + "were connected: " + hiddenSessionNamesConnected,
            hiddenSessionNamesConnected.isEmpty());
        assertTrue("this protection is worthless if the scan simply did nothing at all, so the same run "
                + "must show the scan really did the work it was asked to do: all "
                + OTHER_VISIBLE_SESSION_COUNT + " dead visible sessions must still be reconnected, yet "
                + "these were not: " + otherVisibleSessionNamesNotReconnected,
            otherVisibleSessionNamesNotReconnected.isEmpty());
    }

    @Test
    public void theDisplayedSessionScanCreatesAtMostOneSessionInOneUninterruptedMainThreadPass() {
        runDisplayedSessionScan();
        idleMainThreadTasksDueNow();
        int sessionsCreatedInOneUninterruptedMainThreadPass = sessionCreationCount();

        drainMainThreadQueueToTheScanHorizon();
        int sessionsCreatedOverTheWholeScan = sessionCreationCount();

        assertTrue("the scan runs on the main thread while the owner is looking at the terminal, and "
                + "every session it creates builds a terminal emulator with a full transcript buffer, "
                + "forks and execs a shell and starts three threads for it, so the drawing thread cannot "
                + "render a frame until the pass finishes and the screen freezes for its whole duration; "
                + "the owner's original complaint is that frozen screen, so bringing the displayed "
                + "session back must not be paid for by unpacing the sweep; over a population of one "
                + "displayed, " + OTHER_VISIBLE_SESSION_COUNT + " other visible and " + HIDDEN_SESSION_COUNT
                + " hidden sessions no more than "
                + SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS
                + " session may be created in one uninterrupted main-thread pass, yet "
                + sessionsCreatedInOneUninterruptedMainThreadPass + " were",
            sessionsCreatedInOneUninterruptedMainThreadPass
                <= SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS);

        assertTrue("a scan that creates nothing at all satisfies the bound above without pacing "
                + "anything, so the same run must also show the scan really did the work it was asked to "
                + "do; it must create at least one session by the time the main-thread queue drains, yet "
                + "it created " + sessionsCreatedOverTheWholeScan,
            sessionsCreatedOverTheWholeScan >= 1);
    }

    private void makeTheDisplayedSessionRunningButSilent() throws Exception {
        set(displayedSession, TerminalSession.class, "mShellPid", RUNNING_SHELL_PROCESS_ID);
        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        assertNotNull("the statusline store must exist so a silent session's last output time can be "
            + "recorded", store);
        store.recordStatuslineTimes(DISPLAYED_SESSION_NAME, null,
            System.currentTimeMillis() - SILENT_FOR_LONGER_THAN_THE_RECONNECT_STALENESS_THRESHOLD_MILLIS,
            null);
    }

    private void hideSessionTheWayTheHideControlDoes(String sessionName) {
        preferences.setSessionDisabled(sessionName, true);
    }

    private void runDisplayedSessionScan() {
        scanStartUptimeMillis = SystemClock.uptimeMillis();
        try {
            sessionActivityClient.startDisplayedSessionCallScanTick();
        } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
            throwablesSurfacedByTheScan.add(nativeSubprocessUnavailableOffDevice);
        }
        sessionActivityClient.stopDisplayedSessionCallScanTick();
    }

    private List<String> runBackgroundReconnectScan() {
        scanStartUptimeMillis = SystemClock.uptimeMillis();
        try {
            return sessionActivityClient.reconnectDeadDefinitionBackedSessionsInBackground();
        } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
            throwablesSurfacedByTheScan.add(nativeSubprocessUnavailableOffDevice);
            return new ArrayList<>();
        }
    }

    private void drainMainThreadQueueToTheScanHorizon() {
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis - scanStartUptimeMillis
                    > DRAIN_HORIZON_MILLIS_BEFORE_THE_FIRST_RECONNECT_TIMEOUT) return;
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
                throwablesSurfacedByTheScan.add(nativeSubprocessUnavailableOffDevice);
            }
        }
    }

    private void idleMainThreadTasksDueNow() {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_LIMIT; attempt++) {
            try {
                shadowOf(Looper.getMainLooper()).idle();
                return;
            } catch (LinkageError nativeSubprocessUnavailableOffDevice) {
                throwablesSurfacedByTheScan.add(nativeSubprocessUnavailableOffDevice);
            }
        }
    }

    private void recordThePopulationPresentBeforeTheScan() {
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            sessionsPresentBeforeTheScan.add(terminalSession);
            if (terminalSession.mSessionName != null) {
                sessionNamesPresentBeforeTheScan.add(terminalSession.mSessionName);
            }
        }
    }

    private int sessionCreationCount() {
        int sessionCreationCount = 0;
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            if (!sessionsPresentBeforeTheScan.contains(termuxSession.getTerminalSession())) {
                sessionCreationCount++;
            }
        }
        return sessionCreationCount;
    }

    private Set<String> reconnectedSessionNames() {
        Set<String> reconnectedSessionNames = new LinkedHashSet<>();
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionsPresentBeforeTheScan.contains(terminalSession)) continue;
            if (terminalSession.mSessionName == null) continue;
            reconnectedSessionNames.add(terminalSession.mSessionName);
        }
        return reconnectedSessionNames;
    }

    private Set<String> liveSessionNames() {
        Set<String> liveSessionNames = new LinkedHashSet<>();
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession.mSessionName == null) continue;
            liveSessionNames.add(terminalSession.mSessionName);
        }
        return liveSessionNames;
    }

    private TerminalSession liveSessionNamed(String sessionName) {
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionName.equals(terminalSession.mSessionName)) return terminalSession;
        }
        return null;
    }

    private List<String> otherVisibleSessionNamesNotIn(Set<String> reconnectedSessionNames) {
        List<String> otherVisibleSessionNamesNotReconnected = new ArrayList<>();
        for (TerminalSession otherVisibleSession : otherVisibleSessions) {
            if (!reconnectedSessionNames.contains(otherVisibleSession.mSessionName)) {
                otherVisibleSessionNamesNotReconnected.add(otherVisibleSession.mSessionName);
            }
        }
        return otherVisibleSessionNamesNotReconnected;
    }

    private TerminalSession addDeadSession(String sessionName) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 2000,
                sessionActivityClient);
        terminalSession.mSessionName = sessionName;
        set(terminalSession, TerminalSession.class, "mShellPid", DEAD_SHELL_PROCESS_ID);
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
