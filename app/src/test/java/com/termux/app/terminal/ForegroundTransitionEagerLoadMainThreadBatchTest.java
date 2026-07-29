package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.session.SessionEagerLoadPacer;
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
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ForegroundTransitionEagerLoadMainThreadBatchTest {

    private static final int VISIBLE_SESSION_COUNT = 16;

    private static final int HIDDEN_SESSION_COUNT = 15;

    private static final int MAIN_THREAD_SESSION_INITIALIZATION_BOUND_PER_PASS = 3;

    private TermuxActivity activity;

    private TermuxShellManager shellManager;

    private final List<TerminalSession> visibleSessions = new ArrayList<>();

    private final List<TerminalSession> hiddenSessions = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
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
        terminalView.setTextSize(12);
        terminalView.layout(0, 0, 1080, 1920);
        giveTheViewDeviceLikeFontMetrics(terminalView);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TerminalSession displayedSession = addSession("session-displayed");
        terminalView.mTermSession = displayedSession;

        List<String> hiddenSessionNames = new ArrayList<>();
        for (int index = 1; index <= VISIBLE_SESSION_COUNT; index++) {
            visibleSessions.add(addSession(String.format("session-visible-%02d", index)));
        }
        for (int index = 1; index <= HIDDEN_SESSION_COUNT; index++) {
            String sessionName = String.format("session-hidden-%02d", index);
            hiddenSessions.add(addSession(sessionName));
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(String.join("\n", hiddenSessionNames));
    }

    @Test
    public void foregroundTransitionEagerLoadInitializesEverySessionInOneUninterruptedMainThreadPass() {
        activity.eagerLoadAllSessions();

        int initializedInOneUninterruptedPass = drainMainThreadTasksDueNowAndCountInitializedSessions();

        assertTrue("the eager session load runs on every foreground transition and is the only thing the "
                + "drawing thread can do while it runs; each session it initializes constructs a terminal "
                + "emulator with a 2000-row transcript buffer and then forks and execs an operating system "
                + "process through the native subprocess call and starts three threads for it, so the "
                + "screen cannot be redrawn until the whole pass finishes and the owner sees a black or "
                + "frozen screen for the whole duration; every one of these initializations is posted to "
                + "the main thread handler with a zero delay, so they all become due at the same instant "
                + "and no pacing separates them, unlike the reconnect path, which keeps at most one "
                + "session creation in flight at a time and posts the next only after the previous one "
                + "returns, behind a one-frame yield; over a population of " + VISIBLE_SESSION_COUNT
                + " visible and " + HIDDEN_SESSION_COUNT + " hidden sessions the eager load must not "
                + "initialize more than " + MAIN_THREAD_SESSION_INITIALIZATION_BOUND_PER_PASS
                + " sessions in one uninterrupted main-thread pass, yet it initialized "
                + initializedInOneUninterruptedPass,
            initializedInOneUninterruptedPass <= MAIN_THREAD_SESSION_INITIALIZATION_BOUND_PER_PASS);
    }

    @Test
    public void foregroundTransitionEagerLoadPacesNothingBetweenSuccessiveSessionInitializations() {
        activity.eagerLoadAllSessions();

        long lastScheduledTaskDelayMillis =
            shadowOf(Looper.getMainLooper()).getLastScheduledTaskTime().toMillis()
                - android.os.SystemClock.uptimeMillis();

        assertTrue("the method that performs the eager session load is named as if it staged its work, "
                + "but it posts every session initialization to the main thread handler with a zero "
                + "delay, so the staging is nominal and the last of the "
                + (VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT) + " initializations is due at the same "
                + "instant as the first; for the drawing thread to regain control between process "
                + "creations the load must spread them over time the way the reconnect path does, so the "
                + "last scheduled initialization must not be due at zero delay, yet the last main-thread "
                + "task scheduled by the eager load is due in " + lastScheduledTaskDelayMillis
                + " milliseconds",
            lastScheduledTaskDelayMillis > 0L);
    }

    @Test
    public void foregroundTransitionEagerLoadStillInitializesEveryEagerLoadedSessionOncePacedUnitsHaveDrained() {
        activity.eagerLoadAllSessions();

        drainEveryPacedUnit();

        assertEquals("bounding the uninterrupted pass has to spread the work over time rather than shed "
                + "it, and a count that stays under the per-pass bound is equally satisfied by an eager "
                + "load that initializes nothing at all, so every one of the " + VISIBLE_SESSION_COUNT
                + " eager-loaded sessions must still hold a freshly constructed terminal emulator once "
                + "the paced main-thread units have drained, otherwise a session the owner switches to "
                + "would open uninitialized",
            VISIBLE_SESSION_COUNT, countSessionsHoldingATerminalEmulator(visibleSessions));
        assertEquals("a hidden session's process must stay unstarted, so pacing the eager load must not "
                + "pull any hidden session back into it",
            0, countSessionsHoldingATerminalEmulator(hiddenSessions));
    }

    private int drainMainThreadTasksDueNowAndCountInitializedSessions() {
        ShadowLooper mainLooper = shadowOf(Looper.getMainLooper());
        int attemptBound = VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT + 2;
        boolean drained = false;
        for (int attempt = 0; attempt < attemptBound && !drained; attempt++) {
            try {
                mainLooper.idle();
                drained = true;
            } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
                OffDeviceNativeSubprocessLibrary.assertItIsTheOnlyAbsence(
                    deviceOnlyNativeSubprocessLibraryAbsent);
            }
        }
        assertTrue("every main-thread task due at this instant must have been dispatched before the "
                + "count is taken, otherwise the count understates the uninterrupted pass and the bound "
                + "below is met by a drain that stopped early rather than by pacing; the drain gave up "
                + "after " + attemptBound + " attempts",
            drained);
        return countSessionsHoldingATerminalEmulator(visibleSessions)
            + countSessionsHoldingATerminalEmulator(hiddenSessions);
    }

    private void drainEveryPacedUnit() {
        ShadowLooper mainLooper = shadowOf(Looper.getMainLooper());
        for (int unit = 0; unit < VISIBLE_SESSION_COUNT + HIDDEN_SESSION_COUNT; unit++) {
            try {
                mainLooper.idleFor(Duration.ofMillis(
                    SessionEagerLoadPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS));
            } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
                OffDeviceNativeSubprocessLibrary.assertItIsTheOnlyAbsence(
                    deviceOnlyNativeSubprocessLibraryAbsent);
            }
        }
    }

    private int countSessionsHoldingATerminalEmulator(@NonNull List<TerminalSession> sessions) {
        int initialized = 0;
        for (TerminalSession session : sessions) {
            if (session.getEmulator() != null) initialized++;
        }
        return initialized;
    }

    private void giveTheViewDeviceLikeFontMetrics(TerminalView terminalView) throws Exception {
        Field rendererField = TerminalView.class.getDeclaredField("mRenderer");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(terminalView);
        setFinalField(renderer, "mFontWidth", 10.0f);
        setFinalField(renderer, "mFontLineSpacing", 20);
        setFinalField(renderer, "mFontLineSpacingAndAscent", 15);
    }

    private void setFinalField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private TerminalSession addSession(String sessionName) throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 2000,
                activity.getTermuxTerminalSessionClient());
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
