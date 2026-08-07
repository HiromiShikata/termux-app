package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.JniSpawnCounter;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BulkReconnectEagerInitCountTest {

    private static final int BACKGROUND_SESSION_COUNT = 6;

    private static final long STATUSLINE_CALL_BASE_TIME_MILLIS = 2_000L;

    private static final long STATUSLINE_OUT_TIME_MILLIS = 1_500L;

    private static final long STATUSLINE_REPLY_TIME_MILLIS = 1_000L;

    private static final Duration BEFORE_FIRST_RECONNECT_TIMEOUT =
        Duration.ofMillis(TermuxTerminalSessionActivityClient.RECONNECT_TIMEOUT_MILLIS - 1L);

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TermuxTerminalSessionActivityClient client;

    @Before
    public void setUp() throws Exception {
        JniSpawnCounter.pretendTheDeviceNativeSubprocessLibraryIsPresent();
        JniSpawnCounter.reset();
        assertTrue("the test-source-set JNI stub must be the class on the runtime classpath, "
            + "otherwise the measurement counts nothing", JniSpawnCounter.stubIsActive());

        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        client = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", client);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(24);
        terminalView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        terminalView.layout(0, 0, 1080, 1920);
        forceNonZeroFontMetrics(terminalView);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
        TermuxTerminalViewClient terminalViewClient = new TermuxTerminalViewClient(activity, client);
        set(activity, TermuxActivity.class, "mTermuxTerminalViewClient", terminalViewClient);
        terminalView.setTerminalViewClient(terminalViewClient);

        JniSpawnCounter.reset();
    }

    @After
    public void restoreTheDefaultAbsenceOfTheDeviceNativeSubprocessLibrary() {
        JniSpawnCounter.restoreTheAbsenceOfTheDeviceNativeSubprocessLibrary();
    }

    @Test
    public void bulkDisplayedReconnectLeavesEveryBackgroundSessionEmulatorLazy() throws Exception {
        seedSessions(Collections.<String>emptySet());

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        flushMainLooper();

        assertEquals(measurementDetail("a bulk reconnect of " + BACKGROUND_SESSION_COUNT
                + " dead background sessions must not initialize any of their emulators off-screen, "
                + "because each initialization spawns the ssh/tmux subprocess on the main thread and "
                + "doing that once per reconnected session is what freezes the app"),
            0, JniSpawnCounter.eagerInitCallCount());
        assertEquals(measurementDetail("a bulk reconnect must spawn no subprocess at all"),
            0, JniSpawnCounter.createSubprocessCallCount());
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNull("the replacement session created for " + backgroundSessionName(index)
                    + " must have no emulator until the owner opens it",
                terminalSessionNamed(backgroundSessionName(index)).getEmulator());
        }
    }

    @Test
    public void bulkDisplayedReconnectInitializesOnlyTheSessionCarryingAPendingCall() throws Exception {
        seedSessions(Collections.<String>emptySet());
        service.getSessionNewActivityStore()
            .recordExplicitCall(backgroundSessionName(2), 1_000L, "waiting for the owner");

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        flushMainLooper();

        assertEquals(measurementDetail("exactly the one background session carrying a pending "
                + "call-to-user may be initialized off-screen by a bulk reconnect"),
            1, JniSpawnCounter.eagerInitCallCount());
    }

    @Test
    public void bulkDisplayedReconnectWithEverySessionCallingTheOwnerPreLoadsOnlyTheOldestPendingCall()
        throws Exception {
        seedSessions(Collections.<String>emptySet());
        armStatuslinePendingCallOnEveryBackgroundSession();

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        idleMainLooperFor(BEFORE_FIRST_RECONNECT_TIMEOUT);

        assertEquals(measurementDetail("a bulk reconnect where every one of the "
                + BACKGROUND_SESSION_COUNT + " dead background sessions carries a pending "
                + "call-to-user must still pre-load at most one of them, because a dead session that "
                + "was waiting on the owner can never render a newer reply and so this is the ordinary "
                + "state of the whole population being bulk-reconnected"),
            1, JniSpawnCounter.eagerInitCallCount());
        assertNotNull("the oldest pending call is the one session the sweep pre-loads",
            terminalSessionNamed(backgroundSessionName(0)).getEmulator());
        for (int index = 1; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNull("only the oldest pending call may be pre-loaded, so "
                    + backgroundSessionName(index) + " must stay lazy",
                terminalSessionNamed(backgroundSessionName(index)).getEmulator());
        }
    }

    @Test
    public void bulkDisplayedReconnectWithEverySessionCallingTheOwnerStaysBoundedAcrossTheRetryLadder()
        throws Exception {
        seedSessions(Collections.<String>emptySet());
        armStatuslinePendingCallOnEveryBackgroundSession();

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        flushMainLooper();

        assertEquals(measurementDetail("the reconnect purge preserves the statusline call time, so a "
                + "pending call-to-user survives into every attempt of the automatic retry ladder; the "
                + "whole bulk reconnect of " + BACKGROUND_SESSION_COUNT + " dead background sessions "
                + "must still pre-load one session in total however many attempts it makes"),
            1, JniSpawnCounter.eagerInitCallCount());
        assertEquals(measurementDetail("the bounded pre-load must be the only subprocess the whole bulk "
                + "reconnect spawns"),
            1, JniSpawnCounter.createSubprocessCallCount());
    }

    @Test
    public void eagerLoadAllSessionsAfterTheBoundedSweepStillSpawnsEveryRemainingVisibleBackgroundSession()
        throws Exception {
        seedSessions(Collections.<String>emptySet());
        assertEquals("this case measures the application-level spawn count for a population in which "
                + "every one of the " + BACKGROUND_SESSION_COUNT + " background sessions is visible and "
                + "none is hidden; the foreground eager load collects its own session list, so a hidden "
                + "session would change the number asserted below and the number would no longer stand "
                + "for the whole population",
            Collections.<String>emptySet(), preferences.getDisabledSessionNames());
        armStatuslinePendingCallOnEveryBackgroundSession();

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        flushMainLooper();

        assertEquals(measurementDetail("the bounded sweep is the only spawn source measured so far"),
            1, JniSpawnCounter.createSubprocessCallCount());
        assertEquals(measurementDetail("the one spawn the sweep makes must come from the sweep's own "
                + "pre-load call site"),
            1, JniSpawnCounter.eagerInitCallCount());
        assertEquals(measurementDetail("nothing has driven the foreground eager load yet"),
            0, JniSpawnCounter.eagerLoadAllSessionsCallCount());

        activity.eagerLoadAllSessions();
        flushMainLooper();

        assertEquals(measurementDetail("bounding the sweep does not remove the main-thread spawns: "
                + "TermuxActivity.eagerLoadAllSessions runs on every service connection and on every "
                + "foreground transition, collects the sessions it loads without staggering them, and "
                + "initializes each one, so the application-level spawn count for this all-visible "
                + "fixture is the sweep's one plus one per remaining visible background session"),
            1 + BACKGROUND_SESSION_COUNT, JniSpawnCounter.createSubprocessCallCount());
        assertEquals(measurementDetail("every spawn added after the sweep must be attributed to "
                + "TermuxActivity.eagerLoadSessionEmulator, otherwise this case would pass on a "
                + "count reached from some other call site"),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.eagerLoadAllSessionsCallCount());
        assertEquals(measurementDetail("the sweep's own pre-load count must stay at its bound while the "
                + "foreground eager load runs"),
            1, JniSpawnCounter.eagerInitCallCount());
        assertEquals(measurementDetail("no spawn may be left unattributed to one of the two call sites"),
            JniSpawnCounter.createSubprocessCallCount(),
            JniSpawnCounter.eagerInitCallCount() + JniSpawnCounter.eagerLoadAllSessionsCallCount());
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNotNull("the visible sessions the bounded sweep left lazy regain their process at the "
                    + "next foreground transition without the owner tapping anything, so "
                    + backgroundSessionName(index) + " must be initialized once the foreground eager "
                    + "load has run",
                terminalSessionNamed(backgroundSessionName(index)).getEmulator());
        }
    }

    private void armStatuslinePendingCallOnEveryBackgroundSession() {
        SessionNewActivityStore store = service.getSessionNewActivityStore();
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            store.recordStatuslineTimes(backgroundSessionName(index),
                STATUSLINE_CALL_BASE_TIME_MILLIS + index, STATUSLINE_OUT_TIME_MILLIS,
                STATUSLINE_REPLY_TIME_MILLIS);
        }
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNotNull("every seeded background session must carry a statusline-derived pending "
                    + "call before the sweep runs, otherwise this case measures nothing",
                store.statuslineCallPendingTimeMillis(backgroundSessionName(index)));
        }
    }

    @Test
    public void reloadButtonVisibleSetPathLeavesEveryBackgroundSessionEmulatorLazy() throws Exception {
        seedSessions(allBackgroundSessionNames());

        Set<String> visibleSessionNames = allBackgroundSessionNames();
        visibleSessionNames.add("displayed-session");

        Method method = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(
            "reconnectDeadDefinitionBackedSessionsInBackground", Set.class);
        method.setAccessible(true);
        method.invoke(client, visibleSessionNames);
        flushMainLooper();

        assertEquals(measurementDetail("the reload button sweep must not initialize background session "
                + "emulators off-screen either, whether or not those sessions are hidden"),
            0, JniSpawnCounter.eagerInitCallCount());
    }

    @Test
    public void ownerRequestedFailedRowRetryInitializesExactlyThatSession() throws Exception {
        seedSessions(Collections.<String>emptySet());

        client.retryReconnectAfterFailure(backgroundSessionName(0));
        idleMainLooperFor(BEFORE_FIRST_RECONNECT_TIMEOUT);

        assertEquals(measurementDetail("a failed-row retry the owner tapped must initialize that one "
                + "session off-screen so its process spawns without a further tap"),
            1, JniSpawnCounter.eagerInitCallCount());
        assertNotNull("the session the owner tapped must be the one that was initialized",
            terminalSessionNamed(backgroundSessionName(0)).getEmulator());
    }

    @Test
    public void ownerRequestedRetryLadderKeepsInitializingThatSessionAfterEachTimeout() throws Exception {
        seedSessions(Collections.<String>emptySet());

        client.retryReconnectAfterFailure(backgroundSessionName(0));
        flushMainLooper();

        int ladderAttempts = 1 + TermuxTerminalSessionActivityClient.RECONNECT_RETRY_BACKOFF_MILLIS.length;
        assertEquals(measurementDetail("when the owner-tapped attempt times out, each automatic retry "
                + "must initialize its replacement session too, otherwise the retry creates a session "
                + "that never spawns its process and only another owner tap can resolve the row"),
            ladderAttempts, JniSpawnCounter.eagerInitCallCount());
        assertNotNull("the last replacement the ladder created must be initialized",
            terminalSessionNamed(backgroundSessionName(0)).getEmulator());
        for (int index = 1; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNull("no session other than the one the owner tapped may be initialized by the ladder",
                terminalSessionNamed(backgroundSessionName(index)).getEmulator());
        }
    }

    @Test
    public void ownerRequestedSwitchToDeadSessionInitializesExactlyThatSession() throws Exception {
        seedSessions(Collections.<String>emptySet());

        client.switchToSessionReconnectingIfDead(terminalSessionNamed(backgroundSessionName(0)));
        flushMainLooper();

        assertEquals(measurementDetail("switching to a dead row must initialize the replacement session"),
            1, JniSpawnCounter.eagerInitCallCount());
    }

    private String measurementDetail(String expectation) {
        StringBuilder detail = new StringBuilder(expectation);
        detail.append("\nbackground_sessions=").append(BACKGROUND_SESSION_COUNT)
            .append(" hidden_sessions=").append(preferences.getDisabledSessionNames().size())
            .append(" total_spawns=").append(JniSpawnCounter.createSubprocessCallCount())
            .append(" eager_init_spawns=").append(JniSpawnCounter.eagerInitCallCount())
            .append(" eager_load_all_sessions_spawns=")
            .append(JniSpawnCounter.eagerLoadAllSessionsCallCount());
        for (String site : JniSpawnCounter.callSites()) {
            detail.append("\n  SITE ").append(site);
        }
        return detail.toString();
    }

    private Set<String> allBackgroundSessionNames() {
        Set<String> names = new LinkedHashSet<>();
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            names.add(backgroundSessionName(index));
        }
        return names;
    }

    private void seedSessions(Set<String> hiddenSessionNames) throws Exception {
        TermuxSession displayedSession = deadSession("displayed-session");
        shellManager.mTermuxSessions.add(displayedSession);
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            shellManager.mTermuxSessions.add(deadSession(backgroundSessionName(index)));
        }
        TerminalSession displayed = displayedSession.getTerminalSession();
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(displayed, 4242);
        activity.getTerminalView().mTermSession = displayed;

        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(hiddenSessionNames));
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        for (TermuxSession termuxSession : shellManager.mTermuxSessions) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession != null && sessionName.equals(terminalSession.mSessionName)) {
                return terminalSession;
            }
        }
        throw new IllegalStateException("no seeded session named " + sessionName);
    }

    private static String backgroundSessionName(int index) {
        return "background-session-" + index;
    }

    private void flushMainLooper() {
        idleMainLooperFor(Duration.ofMinutes(5));
    }

    private void idleMainLooperFor(Duration duration) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(duration);
    }

    private void invokePrivate(String methodName) throws Exception {
        Method method = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(client);
    }

    private void forceNonZeroFontMetrics(TerminalView view) throws Exception {
        Object renderer = view.mRenderer;
        assertNotNull("terminal view must have a renderer after a real layout pass", renderer);
        setDeclared(renderer, "mFontWidth", 10f);
        setDeclared(renderer, "mFontLineSpacing", 20);
        setDeclared(renderer, "mFontLineSpacingAndAscent", 15);
    }

    private void setDeclared(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
