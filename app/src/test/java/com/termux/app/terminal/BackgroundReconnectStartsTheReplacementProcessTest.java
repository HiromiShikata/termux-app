package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

@RunWith(RobolectricTestRunner.class)
public class BackgroundReconnectStartsTheReplacementProcessTest {

    private static final int BACKGROUND_SESSION_COUNT = 6;

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
    public void theSweepStartsTheProcessOfEveryBackgroundSessionItReconnects() throws Exception {
        seedSessions();

        invokePrivate("refreshDisplayedSessionsForCallToUser");
        flushMainLooper();

        for (int index = 0; index < BACKGROUND_SESSION_COUNT; index++) {
            TerminalSession replacement = terminalSessionNamed(backgroundSessionName(index));
            assertNotNull("a reconnect that leaves its replacement session without a process has not"
                    + " reconnected anything: the replacement can never write a statusline, so the row"
                    + " runs out its retry ladder and settles on the reload button, and the next"
                    + " background scan finds the process gone and reconnects it again; "
                    + backgroundSessionName(index) + " must therefore own a started process once the"
                    + " sweep has run\n" + measurementDetail(),
                replacement.getEmulator());
            assertTrue("the replacement session for " + backgroundSessionName(index) + " must report"
                    + " itself running, because the reconnecting row is cleared only by output the"
                    + " session produces\n" + measurementDetail(),
                replacement.isRunning());
        }
        assertEquals("every one of the " + BACKGROUND_SESSION_COUNT + " reconnected background"
                + " sessions must account for exactly one spawn made from the reconnect call site\n"
                + measurementDetail(),
            BACKGROUND_SESSION_COUNT, JniSpawnCounter.eagerInitCallCount());
    }

    private String measurementDetail() {
        StringBuilder detail = new StringBuilder();
        detail.append("background_sessions=").append(BACKGROUND_SESSION_COUNT)
            .append(" total_spawns=").append(JniSpawnCounter.createSubprocessCallCount())
            .append(" reconnect_spawns=").append(JniSpawnCounter.eagerInitCallCount())
            .append(" eager_load_all_sessions_spawns=")
            .append(JniSpawnCounter.eagerLoadAllSessionsCallCount());
        for (String site : JniSpawnCounter.callSites()) {
            detail.append("\n  SITE ").append(site);
        }
        return detail.toString();
    }

    private void seedSessions() throws Exception {
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
            TermuxAppSharedPreferences.serializeDisabledSessionNames(Collections.<String>emptySet()));
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
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(5));
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
