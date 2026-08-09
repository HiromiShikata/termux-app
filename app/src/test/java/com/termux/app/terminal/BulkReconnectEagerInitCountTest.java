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

    private static final Duration SETTLE_WITHIN_THE_FIRST_RECONNECT_ATTEMPT =
        Duration.ofMillis(TermuxTerminalSessionActivityClient.RECONNECT_TIMEOUT_MILLIS / 4L);

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
    public void bulkDisplayedReconnectStartsTheProcessOfEveryBackgroundSessionItReconnects()
        throws Exception {
        seedSessions(Collections.<String>emptySet());

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        idleMainLooperFor(SETTLE_WITHIN_THE_FIRST_RECONNECT_ATTEMPT);

        assertEquals(measurementDetail("a bulk reconnect of " + BACKGROUND_SESSION_COUNT
                + " dead background sessions must start the process of every one of them, because a "
                + "replacement session that owns no process writes no statusline, so its row is never "
                + "cleared, runs out the retry ladder and settles on the reload button while the next "
                + "background scan finds the process gone and reconnects it again. The spawns are kept "
                + "off a single main-thread pass by the reconnect pacer, which runs one reconnect per "
                + "main-thread message rather than the whole population in one pass"),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.eagerInitCallCount());
        assertEquals(measurementDetail("no spawn may come from anywhere but the reconnect call site"),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.createSubprocessCallCount());
        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            assertNotNull("the replacement session created for " + backgroundSessionName(index)
                    + " must own a started process without the owner opening it",
                terminalSessionNamed(backgroundSessionName(index)).getEmulator());
        }
    }

    @Test
    public void eagerLoadAllSessionsAfterTheSweepAddsNoFurtherSpawn() throws Exception {
        seedSessions(Collections.<String>emptySet());
        assertEquals("this case measures the application-level spawn count for a population in which "
                + "every one of the " + BACKGROUND_SESSION_COUNT + " background sessions is visible and "
                + "none is hidden; the foreground eager load collects its own session list, so a hidden "
                + "session would change the number asserted below and the number would no longer stand "
                + "for the whole population",
            Collections.<String>emptySet(), preferences.getDisabledSessionNames());

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        idleMainLooperFor(SETTLE_WITHIN_THE_FIRST_RECONNECT_ATTEMPT);

        assertEquals(measurementDetail("the sweep is the only spawn source measured so far"),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.createSubprocessCallCount());
        assertEquals(measurementDetail("nothing has driven the foreground eager load yet"),
            0, JniSpawnCounter.eagerLoadAllSessionsCallCount());

        activity.eagerLoadAllSessions();
        idleMainLooperFor(SETTLE_WITHIN_THE_FIRST_RECONNECT_ATTEMPT);

        assertEquals(measurementDetail("the sweep already started every visible background session's "
                + "process, so the foreground eager load that runs on every service connection and every "
                + "foreground transition finds nothing left to initialize and adds no spawn; the "
                + "application-level count therefore stays at what the paced sweep spent"),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.createSubprocessCallCount());
        assertEquals(measurementDetail("no spawn may be left unattributed to one of the two call sites"),
            JniSpawnCounter.createSubprocessCallCount(),
            JniSpawnCounter.eagerInitCallCount() + JniSpawnCounter.eagerLoadAllSessionsCallCount());
    }

    @Test
    public void reloadButtonVisibleSetPathLeavesEveryHiddenBackgroundSessionUnreconnected()
        throws Exception {
        seedSessions(allBackgroundSessionNames());

        Set<String> visibleSessionNames = allBackgroundSessionNames();
        visibleSessionNames.add("displayed-session");

        Method method = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(
            "reconnectDeadDefinitionBackedSessionsInBackground", Set.class);
        method.setAccessible(true);
        method.invoke(client, visibleSessionNames);
        flushMainLooper();

        assertEquals(measurementDetail("every background session in this fixture is hidden, and a hidden "
                + "session is refused a reconnect outright, so the reload button sweep must start no "
                + "process at all"),
            0, JniSpawnCounter.eagerInitCallCount());
    }

    @Test
    public void ownerRequestedFailedRowRetryInitializesExactlyThatSession() throws Exception {
        seedSessions(Collections.<String>emptySet());

        client.retryReconnectAfterFailure(backgroundSessionName(0));
        idleMainLooperFor(SETTLE_WITHIN_THE_FIRST_RECONNECT_ATTEMPT);

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
