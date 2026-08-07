package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
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
public class DisplayedSessionReconnectPreservesDisplayedContextTest {

    private static final String DISPLAYED_SESSION_NAME = "session-displayed";

    private static final int OTHER_VISIBLE_SESSION_COUNT = 2;

    private static final int DEAD_SHELL_PROCESS_ID = -1;

    private static final String UNSUBMITTED_TOOLBAR_INPUT =
        "git commit -m \"the owner typed this and has not pressed enter yet\"";

    private static final long DRAIN_HORIZON_MILLIS_BEFORE_THE_FIRST_RECONNECT_TIMEOUT = 20_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final int DRAIN_ATTEMPT_LIMIT = 2_000;

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private TerminalView terminalView;

    private EditText toolbarTextInput;

    private TerminalSession displayedSession;

    private final List<TerminalSession> otherVisibleSessions = new ArrayList<>();

    private final Set<TerminalSession> sessionsPresentBeforeTheScan = new LinkedHashSet<>();

    private final List<Throwable> throwablesSurfacedByTheScan = new ArrayList<>();

    private long scanStartUptimeMillis;

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
        drawerLayout.addView(buildTerminalToolbar(appContext));
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        displayedSession = addDeadSession(DISPLAYED_SESSION_NAME);
        terminalView.mTermSession = displayedSession;

        for (int index = 1; index <= OTHER_VISIBLE_SESSION_COUNT; index++) {
            otherVisibleSessions.add(addDeadSession(String.format("session-visible-%02d", index)));
        }

        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            sessionsPresentBeforeTheScan.add(termuxSession.getTerminalSession());
        }
        idleMainThreadTasksDueNow();
    }

    @Test
    public void theDisplayedSessionScanKeepsTheOwnersUnsubmittedToolbarInputWhenItReplacesTheDisplayedSession() {
        toolbarTextInput.setText(UNSUBMITTED_TOOLBAR_INPUT);

        runDisplayedSessionScan();
        drainMainThreadQueueToTheScanHorizon();

        TerminalSession replacementSession = liveSessionNamed(DISPLAYED_SESSION_NAME);
        assertNotNull("this test only says something about carrying the owner's typing across the "
            + "reconnect if the reconnect really happened, so a live session named "
            + DISPLAYED_SESSION_NAME + " must exist once the main-thread queue drains"
            + caughtErrorsDescription(),
            replacementSession);
        assertNotSame("the reconnect must really have replaced the displayed session object, otherwise "
            + "nothing was carried across anything", displayedSession, replacementSession);

        assertEquals("the owner types into the toolbar text field and presses enter later, so text he has "
                + "not submitted yet is the only copy of it; the reconnect of the displayed session now "
                + "happens on its own from a background tick rather than only when he taps a row, so it "
                + "must not throw that text away: the toolbar stash is keyed by the session object, the "
                + "dead session's entry is dropped while it is torn down, and unless the live text is "
                + "carried to the replacement the restore for the replacement finds nothing and clears "
                + "the field under his fingers",
            UNSUBMITTED_TOOLBAR_INPUT, toolbarTextInput.getText().toString());
    }

    @Test
    public void theDisplayedSessionScanLeavesTheTerminalViewBoundToTheLiveReplacementSession() {
        runDisplayedSessionScan();
        drainMainThreadQueueToTheScanHorizon();

        TerminalSession replacementSession = liveSessionNamed(DISPLAYED_SESSION_NAME);
        assertNotNull("the reconnect removes the dead displayed session before it creates its "
            + "replacement, so a live session named " + DISPLAYED_SESSION_NAME + " must exist once the "
            + "main-thread queue drains" + caughtErrorsDescription(), replacementSession);

        assertSame("the terminal view is the owner's whole window onto the session, so after the "
                + "displayed session is reconnected it must be attached to the live replacement rather "
                + "than left holding the session object that was just removed from the service and "
                + "released; a view still bound to the replaced object shows a terminal that can never "
                + "receive output or accept a keystroke again, which is worse for the owner than the dead "
                + "session he started with",
            replacementSession, terminalView.mTermSession);
        assertNotSame("the session the terminal view is attached to must not be the replaced object",
            displayedSession, terminalView.mTermSession);
        assertTrue("the session the terminal view is attached to must be one the service still lists, "
            + "because a session absent from that list is reachable by no close, reconnect or output path",
            liveSessions().contains(terminalView.mTermSession));
    }

    private FrameLayout buildTerminalToolbar(Context appContext) throws Exception {
        FrameLayout terminalToolbar = new FrameLayout(appContext);

        toolbarTextInput = new EditText(appContext);
        toolbarTextInput.setId(R.id.terminal_toolbar_text_input);
        terminalToolbar.addView(toolbarTextInput);

        ViewPager toolbarViewPager = new ViewPager(appContext);
        toolbarViewPager.setId(R.id.terminal_toolbar_view_pager);
        set(toolbarViewPager, ViewPager.class, "mAdapter",
            new TerminalToolbarViewPager.PageAdapter(activity, null));
        terminalToolbar.addView(toolbarViewPager);

        return terminalToolbar;
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
        throw failureFromCaughtErrors("the main-thread clock never reached uptime "
            + targetUptimeMillis + " because all " + DRAIN_ATTEMPT_LIMIT
            + " attempts to idle it threw");
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
        throw failureFromCaughtErrors("the tasks already due on the main thread never ran because all "
            + DRAIN_ATTEMPT_LIMIT + " attempts to idle it threw");
    }

    private AssertionError failureFromCaughtErrors(String whatDidNotHappen) {
        Throwable lastCaughtError = throwablesSurfacedByTheScan.isEmpty()
            ? null
            : throwablesSurfacedByTheScan.get(throwablesSurfacedByTheScan.size() - 1);
        return new AssertionError(whatDidNotHappen + caughtErrorsDescription(), lastCaughtError);
    }

    private String caughtErrorsDescription() {
        if (throwablesSurfacedByTheScan.isEmpty()) return "";
        StringBuilder description =
            new StringBuilder("; errors caught and stepped over while the scan ran:");
        for (Throwable caughtError : throwablesSurfacedByTheScan) {
            description.append(' ').append(caughtError);
        }
        return description.toString();
    }

    private Set<TerminalSession> liveSessions() {
        Set<TerminalSession> liveSessions = new LinkedHashSet<>();
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            liveSessions.add(termuxSession.getTerminalSession());
        }
        return liveSessions;
    }

    private TerminalSession liveSessionNamed(String sessionName) {
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionName.equals(terminalSession.mSessionName)) return terminalSession;
        }
        return null;
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
