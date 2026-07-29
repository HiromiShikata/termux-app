package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;

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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BackgroundReconnectSweepMainThreadPacingTest {

    private static final int DEAD_SESSION_COUNT = 24;

    private static final int SESSION_CREATIONS_ALLOWED_PER_UNINTERRUPTED_MAIN_THREAD_PASS = 1;

    private static final long DRAIN_WINDOW_MILLIS = 20_000L;

    private static final long FIRST_UNIT_OBSERVATION_WINDOW_MILLIS = 64L;

    private static final int DRAIN_ITERATION_CAP = 4 * DEAD_SESSION_COUNT + 64;

    private TermuxActivity activity;

    private TermuxService service;

    private TermuxShellManager shellManager;

    private TerminalView terminalView;

    private final Set<TerminalSession> sessionsPresentBeforeTheSweep =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private final List<TerminalSession> deadSessions = new ArrayList<>();

    private final Map<Long, Integer> sessionCreationCountByDueUptimeMillis = new LinkedHashMap<>();

    private int recordedSessionCreationCount;

    private long sweepStartUptimeMillis;

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
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TerminalSession foregroundSession = addLiveSession("session-foreground");
        terminalView.mTermSession = foregroundSession;

        for (int sessionNumber = 1; sessionNumber <= DEAD_SESSION_COUNT; sessionNumber++) {
            deadSessions.add(addDeadSession(String.format("session-dead-%02d", sessionNumber)));
        }
        for (TermuxSession termuxSession : shellManager.mTermuxSessions) {
            sessionsPresentBeforeTheSweep.add(termuxSession.getTerminalSession());
        }
    }

    @Test
    public void theBackgroundReconnectSweepCreatesAtMostOneSessionInOneUninterruptedMainThreadPass() {
        startTheBackgroundReconnectSweep();

        int sessionsCreatedInOneUninterruptedPass = runEveryMainThreadTaskDueNow();

        assertTrue("the background reconnect sweep runs on the main thread and is the only thing the "
                + "drawing thread can do while it runs; every session it creates constructs a terminal "
                + "emulator with a 2000-row transcript buffer, forks and executes a shell through the "
                + "native subprocess call and starts three threads for it, so the terminal screen stays "
                + "on its previous frame or blank until the whole pass finishes; over a population of "
                + DEAD_SESSION_COUNT + " dead sessions the sweep must create no more than "
                + SESSION_CREATIONS_ALLOWED_PER_UNINTERRUPTED_MAIN_THREAD_PASS
                + " session in one uninterrupted main-thread pass, yet it created "
                + sessionsCreatedInOneUninterruptedPass
                + "; a pass that creates nothing at all would satisfy this bound trivially, which is why "
                + "theBackgroundReconnectSweepStillReconnectsEverySelectedSessionOnceTheQueueDrains "
                + "separately requires every selected session to be reconnected",
            sessionsCreatedInOneUninterruptedPass
                <= SESSION_CREATIONS_ALLOWED_PER_UNINTERRUPTED_MAIN_THREAD_PASS);
    }

    @Test
    public void theBackgroundReconnectSweepMakesNoTwoSessionCreationsDueAtTheSameInstant() {
        startTheBackgroundReconnectSweep();
        drainEveryPacedUnitWithinTheDrainWindow();

        int largestNumberOfSessionCreationsSharingOneInstant =
            largestNumberOfSessionCreationsSharingOneInstant();

        assertEquals("the sweep stamps every unit's delay against the same instant before any unit has "
                + "run, so a whole slot of units becomes due together and the looper drains them back to "
                + "back with no frame in between; for the drawing thread to render between two session "
                + "creations the sweep must schedule each one only after the previous returned, so no "
                + "instant may carry more than one creation, yet the creations became due as "
                + describeSessionCreationsByDueInstant() + " and the last one was due "
                + lastSessionCreationDueMillisAfterTheSweepStarted()
                + " milliseconds after the sweep started",
            1, largestNumberOfSessionCreationsSharingOneInstant);
    }

    @Test
    public void theBackgroundReconnectSweepStillReconnectsEverySelectedSessionOnceTheQueueDrains() {
        List<String> selectedSessionNames = everySelectedSessionName();
        startTheBackgroundReconnectSweep();
        drainEveryPacedUnitWithinTheDrainWindow();

        assertEquals("pacing the sweep has to spread the session creations over time rather than shed "
                + "them, because a per-pass bound is equally satisfied by a sweep that reconnects nothing "
                + "at all and the owner would then be left looking at dead sessions that never come back",
            selectedSessionNames, createdReplacementSessionNames());
    }

    @Test
    public void theBackgroundReconnectSweepKeepsDrainingAfterOneSessionCreationThrowsAndLetsTheThrowReachTheCaller() {
        layOutTheTerminalViewSoSessionCreationReachesTheDeviceOnlyNativeSubprocessCall();
        List<String> selectedSessionNames = everySelectedSessionName();

        LinkageError throwReachingTheCaller = assertThrows(LinkageError.class,
            this::startTheSweepAndRunItsFirstUnitWithoutAbsorbingFailures);
        assertOnlyTheDeviceOnlyNativeSubprocessLibraryIsAbsent(throwReachingTheCaller);

        drainEveryPacedUnitWithinTheDrainWindow();

        assertEquals("a session whose process creation fails must not strand every session queued behind "
                + "it, because the owner's device is exactly the population where a process creation can "
                + "fail and a stalled queue leaves every remaining dead session unreconnected until some "
                + "later sweep happens to revive it",
            selectedSessionNames, createdReplacementSessionNames());
    }

    @Test
    public void theBackgroundReconnectSweepDoesNotCreateASessionThatLeftTheListBeforeItsUnitRan() {
        List<String> selectedSessionNames = everySelectedSessionName();
        startTheBackgroundReconnectSweep();
        TerminalSession sessionLeavingTheList = deadSessions.get(deadSessions.size() - 1);
        String nameOfTheSessionLeavingTheList = sessionLeavingTheList.mSessionName;
        removeFromTheServiceWithoutReconnecting(sessionLeavingTheList);

        drainEveryPacedUnitWithinTheDrainWindow();

        List<String> expectedReplacementSessionNames = new ArrayList<>(selectedSessionNames);
        expectedReplacementSessionNames.remove(nameOfTheSessionLeavingTheList);
        assertEquals("pacing the sweep opens a window in which the session list changes under it, so a "
                + "session the owner closed after the sweep enqueued it must not have a replacement shell "
                + "forked for it; creating one resurrects a session the owner deliberately closed and "
                + "leaves a process that no close path will ever terminate",
            expectedReplacementSessionNames, createdReplacementSessionNames());
    }

    @NonNull
    private List<String> everySelectedSessionName() {
        List<String> selectedSessionNames = new ArrayList<>();
        for (TerminalSession deadSession : deadSessions) selectedSessionNames.add(deadSession.mSessionName);
        return selectedSessionNames;
    }

    private void startTheBackgroundReconnectSweep() {
        sweepStartUptimeMillis = SystemClock.uptimeMillis();
        activity.getTermuxTerminalSessionClient().startDisplayedSessionCallScanTick();
        recordSessionCreationsBecomingDueNow();
    }

    private int runEveryMainThreadTaskDueNow() {
        advanceTheMainThreadTo(SystemClock.uptimeMillis());
        recordSessionCreationsBecomingDueNow();
        return recordedSessionCreationCount;
    }

    private void startTheSweepAndRunItsFirstUnitWithoutAbsorbingFailures() {
        startTheBackgroundReconnectSweep();
        shadowOf(Looper.getMainLooper())
            .idleFor(Duration.ofMillis(FIRST_UNIT_OBSERVATION_WINDOW_MILLIS));
    }

    private void drainEveryPacedUnitWithinTheDrainWindow() {
        ShadowLooper mainLooper = shadowOf(Looper.getMainLooper());
        long drainDeadlineUptimeMillis = SystemClock.uptimeMillis() + DRAIN_WINDOW_MILLIS;
        for (int iteration = 0; iteration < DRAIN_ITERATION_CAP; iteration++) {
            long nextDueUptimeMillis = mainLooper.getNextScheduledTaskTime().toMillis();
            if (nextDueUptimeMillis == 0L) return;
            if (nextDueUptimeMillis > drainDeadlineUptimeMillis) return;
            advanceTheMainThreadTo(nextDueUptimeMillis);
            recordSessionCreationsBecomingDueNow();
        }
    }

    private void advanceTheMainThreadTo(long targetUptimeMillis) {
        long advanceMillis = Math.max(0L, targetUptimeMillis - SystemClock.uptimeMillis());
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(advanceMillis));
        } catch (LinkageError deviceOnlyNativeSubprocessLibraryAbsent) {
            assertOnlyTheDeviceOnlyNativeSubprocessLibraryIsAbsent(deviceOnlyNativeSubprocessLibraryAbsent);
        }
    }

    private void recordSessionCreationsBecomingDueNow() {
        int createdSessionCount = createdReplacementSessionNames().size();
        int newlyCreatedSessionCount = createdSessionCount - recordedSessionCreationCount;
        if (newlyCreatedSessionCount <= 0) return;
        recordedSessionCreationCount = createdSessionCount;
        sessionCreationCountByDueUptimeMillis.merge(
            SystemClock.uptimeMillis() - sweepStartUptimeMillis, newlyCreatedSessionCount, Integer::sum);
    }

    private int largestNumberOfSessionCreationsSharingOneInstant() {
        int largest = 0;
        for (int creationsAtOneInstant : sessionCreationCountByDueUptimeMillis.values()) {
            largest = Math.max(largest, creationsAtOneInstant);
        }
        return largest;
    }

    @NonNull
    private String describeSessionCreationsByDueInstant() {
        StringBuilder description = new StringBuilder();
        for (Map.Entry<Long, Integer> entry : sessionCreationCountByDueUptimeMillis.entrySet()) {
            if (description.length() > 0) description.append(", ");
            description.append(entry.getValue()).append(" at ").append(entry.getKey()).append("ms");
        }
        return description.length() == 0 ? "no creations at all" : description.toString();
    }

    private long lastSessionCreationDueMillisAfterTheSweepStarted() {
        long lastDueMillis = -1L;
        for (Long dueMillis : sessionCreationCountByDueUptimeMillis.keySet()) lastDueMillis = dueMillis;
        return lastDueMillis;
    }

    @NonNull
    private List<String> createdReplacementSessionNames() {
        List<String> createdReplacementSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) continue;
            if (sessionsPresentBeforeTheSweep.contains(terminalSession)) continue;
            createdReplacementSessionNames.add(terminalSession.mSessionName);
        }
        return createdReplacementSessionNames;
    }

    private void removeFromTheServiceWithoutReconnecting(@NonNull TerminalSession session) {
        for (TermuxSession termuxSession : new ArrayList<>(shellManager.mTermuxSessions)) {
            if (termuxSession.getTerminalSession() == session) {
                shellManager.mTermuxSessions.remove(termuxSession);
                return;
            }
        }
    }

    private void layOutTheTerminalViewSoSessionCreationReachesTheDeviceOnlyNativeSubprocessCall()
            throws RuntimeException {
        try {
            Field rendererField = TerminalView.class.getDeclaredField("mRenderer");
            rendererField.setAccessible(true);
            Object renderer = rendererField.get(terminalView);
            setFinalField(renderer, "mFontWidth", 10.0f);
            setFinalField(renderer, "mFontLineSpacing", 20);
            setFinalField(renderer, "mFontLineSpacingAndAscent", 15);
            TerminalSession sessionAttachedToTheView = terminalView.mTermSession;
            terminalView.mTermSession = null;
            terminalView.layout(0, 0, 1080, 1920);
            terminalView.mTermSession = sessionAttachedToTheView;
        } catch (Exception reflectionFailure) {
            throw new RuntimeException(reflectionFailure);
        }
    }

    private void assertOnlyTheDeviceOnlyNativeSubprocessLibraryIsAbsent(@NonNull LinkageError error) {
        Throwable rootCause = error;
        while (rootCause.getCause() != null) rootCause = rootCause.getCause();
        String absorbedFailure = rootCause.getClass().getName() + ": " + rootCause.getMessage();
        assertTrue("a Java virtual machine run can only absorb the absence of the device-only native "
                + "subprocess library that TerminalSession.initializeEmulator loads after it has already "
                + "constructed the terminal emulator; every other failure is a real one and must surface "
                + "instead of being discarded, yet this run absorbed " + absorbedFailure,
            absorbedFailure.contains("UnsatisfiedLinkError")
                || absorbedFailure.contains("com.termux.terminal.JNI"));
    }

    @NonNull
    private TerminalSession addLiveSession(@NonNull String sessionName) throws Exception {
        return addSession(sessionName, 1);
    }

    @NonNull
    private TerminalSession addDeadSession(@NonNull String sessionName) throws Exception {
        return addSession(sessionName, -1);
    }

    @NonNull
    private TerminalSession addSession(@NonNull String sessionName, int shellPid) throws Exception {
        TerminalSession terminalSession = new TerminalSession("/system/bin/sh", "/", new String[0],
            new String[0], 2000, activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field shellPidField = TerminalSession.class.getDeclaredField("mShellPid");
        shellPidField.setAccessible(true);
        shellPidField.setInt(terminalSession, shellPid);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        return terminalSession;
    }

    private void setFinalField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
