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
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BackgroundReconnectSweepMainThreadPacingTest {

    private static final int VISIBLE_SESSION_COUNT = 16;

    private static final int HIDDEN_SESSION_COUNT = 15;

    private static final int SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS = 1;

    private static final int SESSION_CREATION_BOUND_PER_DUE_INSTANT = 1;

    private static final long DRAIN_HORIZON_MILLIS_BEFORE_THE_FIRST_RECONNECT_TIMEOUT = 20_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final int DRAIN_ATTEMPT_LIMIT = 2_000;

    private static final int DEAD_SHELL_PROCESS_ID = -1;

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private TerminalView terminalView;

    private TerminalSession displayedSession;

    private final List<TerminalSession> visibleSessions = new ArrayList<>();

    private final List<TerminalSession> hiddenSessions = new ArrayList<>();

    private final Set<TerminalSession> sessionsPresentBeforeTheSweep = new LinkedHashSet<>();

    private final List<Throwable> throwablesSurfacedByTheSweep = new ArrayList<>();

    private final List<Long> sessionCreationInstantsSinceTheSweepStarted = new ArrayList<>();

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

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        displayedSession = addSession("session-displayed");
        terminalView.mTermSession = displayedSession;

        List<String> hiddenSessionNames = new ArrayList<>();
        for (int index = 1; index <= VISIBLE_SESSION_COUNT; index++) {
            visibleSessions.add(addDeadSession(String.format("session-visible-%02d", index)));
        }
        for (int index = 1; index <= HIDDEN_SESSION_COUNT; index++) {
            String sessionName = String.format("session-hidden-%02d", index);
            hiddenSessions.add(addDeadSession(sessionName));
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(String.join("\n", hiddenSessionNames));

        for (TermuxSession termuxSession : shellManager.mTermuxSessions) {
            sessionsPresentBeforeTheSweep.add(termuxSession.getTerminalSession());
        }
        idleMainThreadTasksDueNow();
    }

    @Test
    public void backgroundReconnectSweepCreatesAtMostOneSessionInOneUninterruptedMainThreadPass() {
        runBackgroundReconnectSweep();
        int sessionsCreatedInsideTheSchedulingPass = sessionCreationCount();

        idleMainThreadTasksDueNow();
        recordSessionCreationInstants();
        int sessionsCreatedInOneUninterruptedMainThreadPass = sessionCreationCount();

        drainMainThreadQueueToTheSweepHorizon();
        int sessionsCreatedOverTheWholeSweep = sessionCreationCount();

        assertTrue("the background reconnect sweep runs on the main thread while the owner is looking at "
                + "the terminal, and every session it creates constructs a terminal emulator with a "
                + "2000-row transcript buffer, forks and execs a shell through the native subprocess "
                + "call and starts three threads for it, so the drawing thread cannot render a frame "
                + "until the whole pass finishes and the screen stays on its previous frame or blank for "
                + "the whole duration; over a population of " + VISIBLE_SESSION_COUNT + " visible and "
                + HIDDEN_SESSION_COUNT + " hidden sessions the sweep must keep at most "
                + SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS
                + " session creation in flight and must therefore create no more than "
                + SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS
                + " session in one uninterrupted main-thread pass, yet it created "
                + sessionsCreatedInsideTheSchedulingPass
                + " inline inside its own scheduling pass and "
                + sessionsCreatedInOneUninterruptedMainThreadPass
                + " by the end of that pass once the main-thread tasks already due had run",
            sessionsCreatedInOneUninterruptedMainThreadPass
                <= SESSION_CREATION_BOUND_PER_UNINTERRUPTED_MAIN_THREAD_PASS);

        assertTrue("a sweep that creates nothing at all satisfies the bound above without pacing "
                + "anything, so the same run must also show the sweep really did the work it was asked "
                + "to do; over a population of " + VISIBLE_SESSION_COUNT + " visible and "
                + HIDDEN_SESSION_COUNT + " hidden sessions the sweep must create at least one session "
                + "by the time the main-thread queue drains, yet it created "
                + sessionsCreatedOverTheWholeSweep,
            sessionsCreatedOverTheWholeSweep >= 1);
    }

    @Test
    public void backgroundReconnectSweepDoesNotMakeSeveralSessionCreationsDueAtTheSameInstant() {
        runBackgroundReconnectSweep();
        drainMainThreadQueueToTheSweepHorizon();

        long instantCarryingTheMostCreations = 0L;
        int mostCreationsCarriedByOneInstant = 0;
        for (Long creationInstant : sessionCreationInstantsSinceTheSweepStarted) {
            int creationsAtThisInstant = 0;
            for (Long otherCreationInstant : sessionCreationInstantsSinceTheSweepStarted) {
                if (otherCreationInstant.equals(creationInstant)) creationsAtThisInstant++;
            }
            if (creationsAtThisInstant > mostCreationsCarriedByOneInstant) {
                mostCreationsCarriedByOneInstant = creationsAtThisInstant;
                instantCarryingTheMostCreations = creationInstant;
            }
        }
        long lastSessionCreationInstant = sessionCreationInstantsSinceTheSweepStarted.isEmpty()
            ? 0L
            : sessionCreationInstantsSinceTheSweepStarted
                .get(sessionCreationInstantsSinceTheSweepStarted.size() - 1);

        assertTrue("the sweep computes every start delay up front against one single instant before any "
                + "creation has run, so its first creations share the instant the sweep itself runs on "
                + "and each later slot releases a whole group that becomes due together, which the "
                + "looper then drains back to back with no yield for the drawing thread; the next "
                + "creation must instead be scheduled only after the previous one has returned, behind a "
                + "yield that lets the drawing thread render a frame, so no instant may carry more than "
                + SESSION_CREATION_BOUND_PER_DUE_INSTANT + " creation; over a population of "
                + VISIBLE_SESSION_COUNT + " visible and " + HIDDEN_SESSION_COUNT + " hidden sessions the "
                + "sweep made " + sessionCreationInstantsSinceTheSweepStarted.size()
                + " creations whose last one was due " + lastSessionCreationInstant
                + " milliseconds after the sweep began, and " + mostCreationsCarriedByOneInstant
                + " of them were due together at the single instant " + instantCarryingTheMostCreations
                + " milliseconds after the sweep began; the creation instants were "
                + sessionCreationInstantsSinceTheSweepStarted,
            mostCreationsCarriedByOneInstant <= SESSION_CREATION_BOUND_PER_DUE_INSTANT);

        assertTrue("a sweep that creates nothing at all leaves no instant carrying a creation and "
                + "satisfies the bound above without pacing anything, so the same run must also show the "
                + "sweep really did the work it was asked to do; over a population of "
                + VISIBLE_SESSION_COUNT + " visible and " + HIDDEN_SESSION_COUNT + " hidden sessions the "
                + "sweep must create at least one session by the time the main-thread queue drains, yet "
                + "it created " + sessionCreationInstantsSinceTheSweepStarted.size(),
            !sessionCreationInstantsSinceTheSweepStarted.isEmpty());
    }

    @Test
    public void backgroundReconnectSweepReconnectsEverySelectedSessionOnceTheMainThreadQueueDrains() {
        runBackgroundReconnectSweep();
        drainMainThreadQueueToTheSweepHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        List<String> selectedSessionNamesNotReconnected =
            selectedSessionNamesNotIn(reconnectedSessionNames);

        assertTrue("pacing the sweep must change only when each session is reconnected, never whether it "
                + "is reconnected at all, otherwise the pacing would be satisfied by silently doing less "
                + "work and the owner would be left with dead sessions that never come back; every one "
                + "of the " + VISIBLE_SESSION_COUNT + " dead visible sessions the sweep selects must be "
                + "reconnected once the main-thread queue drains, yet " + reconnectedSessionNames.size()
                + " were reconnected and these were not: " + selectedSessionNamesNotReconnected,
            selectedSessionNamesNotReconnected.isEmpty());
    }

    @Test
    public void backgroundReconnectSweepKeepsTheSessionsBehindAThrowingCreationScheduledAndLetsTheThrowReachTheCaller()
            throws Exception {
        layOutTheTerminalViewSoEveryBackgroundCreationInitializesAnEmulator();

        runBackgroundReconnectSweep();
        drainMainThreadQueueToTheSweepHorizon();

        Set<String> reconnectedSessionNames = reconnectedSessionNames();
        List<String> selectedSessionNamesNotReconnected =
            selectedSessionNamesNotIn(reconnectedSessionNames);

        assertTrue("a session creation that throws must not strand the sessions queued behind it, "
                + "because the owner would otherwise be left with the rest of the population dead until "
                + "some later sweep; the sweep runs its first creations inline inside its own scheduling "
                + "loop, so a throw unwinds that loop and the remainder is never scheduled at all; over "
                + "a population of " + VISIBLE_SESSION_COUNT + " visible and " + HIDDEN_SESSION_COUNT
                + " hidden sessions, with the terminal view laid out so every background creation "
                + "initializes an emulator and then fails on the native subprocess call, all "
                + VISIBLE_SESSION_COUNT + " selected sessions must still be reconnected once the queue "
                + "drains, yet " + reconnectedSessionNames.size() + " were reconnected and these were "
                + "not: " + selectedSessionNamesNotReconnected,
            selectedSessionNamesNotReconnected.isEmpty());

        assertTrue("a session creation that throws must not have its failure swallowed, because a "
                + "swallowed failure hides a session that never came back behind a sweep that looks "
                + "successful; the throw must reach the caller of the creation, yet over the whole sweep "
                + throwablesSurfacedByTheSweep.size() + " throwables reached the caller",
            !throwablesSurfacedByTheSweep.isEmpty());
    }

    private void runBackgroundReconnectSweep() {
        sweepStartUptimeMillis = SystemClock.uptimeMillis();
        try {
            sessionActivityClient.startDisplayedSessionCallScanTick();
        } catch (Throwable surfacedByTheSweep) {
            throwablesSurfacedByTheSweep.add(surfacedByTheSweep);
        }
        sessionActivityClient.stopDisplayedSessionCallScanTick();
        recordSessionCreationInstants();
    }

    private void drainMainThreadQueueToTheSweepHorizon() {
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis - sweepStartUptimeMillis
                    > DRAIN_HORIZON_MILLIS_BEFORE_THE_FIRST_RECONNECT_TIMEOUT) return;
            advanceMainThreadClockTo(nextTaskDueUptimeMillis);
            recordSessionCreationInstants();
        }
    }

    private void advanceMainThreadClockTo(long targetUptimeMillis) {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_LIMIT; attempt++) {
            long remainingMillis = Math.max(0L, targetUptimeMillis - SystemClock.uptimeMillis());
            try {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(remainingMillis));
                return;
            } catch (Throwable nativeSubprocessUnavailableOffDevice) {
                throwablesSurfacedByTheSweep.add(nativeSubprocessUnavailableOffDevice);
                recordSessionCreationInstants();
            }
        }
    }

    private void idleMainThreadTasksDueNow() {
        for (int attempt = 0; attempt < DRAIN_ATTEMPT_LIMIT; attempt++) {
            try {
                shadowOf(Looper.getMainLooper()).idle();
                return;
            } catch (Throwable nativeSubprocessUnavailableOffDevice) {
                throwablesSurfacedByTheSweep.add(nativeSubprocessUnavailableOffDevice);
                recordSessionCreationInstants();
            }
        }
    }

    private void recordSessionCreationInstants() {
        long instantSinceTheSweepStarted = SystemClock.uptimeMillis() - sweepStartUptimeMillis;
        int sessionCreationCount = sessionCreationCount();
        while (sessionCreationInstantsSinceTheSweepStarted.size() < sessionCreationCount) {
            sessionCreationInstantsSinceTheSweepStarted.add(instantSinceTheSweepStarted);
        }
    }

    private int sessionCreationCount() {
        int sessionCreationCount = 0;
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            if (!sessionsPresentBeforeTheSweep.contains(termuxSession.getTerminalSession())) {
                sessionCreationCount++;
            }
        }
        return sessionCreationCount;
    }

    private Set<String> reconnectedSessionNames() {
        Set<String> reconnectedSessionNames = new LinkedHashSet<>();
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionsPresentBeforeTheSweep.contains(terminalSession)) continue;
            if (terminalSession.mSessionName == null) continue;
            reconnectedSessionNames.add(terminalSession.mSessionName);
        }
        return reconnectedSessionNames;
    }

    private List<String> selectedSessionNamesNotIn(Set<String> reconnectedSessionNames) {
        List<String> selectedSessionNamesNotReconnected = new ArrayList<>();
        for (TerminalSession visibleSession : visibleSessions) {
            if (!reconnectedSessionNames.contains(visibleSession.mSessionName)) {
                selectedSessionNamesNotReconnected.add(visibleSession.mSessionName);
            }
        }
        return selectedSessionNamesNotReconnected;
    }

    private void layOutTheTerminalViewSoEveryBackgroundCreationInitializesAnEmulator() throws Exception {
        giveTheViewDeviceLikeFontMetrics(terminalView);
        terminalView.mTermSession = null;
        terminalView.layout(0, 0, 1080, 1920);
        terminalView.mTermSession = displayedSession;
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

    private TerminalSession addDeadSession(String sessionName) throws Exception {
        TerminalSession terminalSession = addSession(sessionName);
        set(terminalSession, TerminalSession.class, "mShellPid", DEAD_SHELL_PROCESS_ID);
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
