package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(
    instrumentedPackages = {"com.termux.app.terminal.BackgroundOutputTagScanner"},
    shadows = {DisplayedSessionCallToUserGateSeesThisRenderTest.GateRecordingOutputTagScannerShadow.class})
public class DisplayedSessionCallToUserGateSeesThisRenderTest {

    private static final String SESSION_NAME = "session-calling-the-owner";

    private static final int LIVE_SHELL_PROCESS_ID = 4242;

    private static final int TRANSCRIPT_ROWS = 2000;

    private static final int SCREEN_COLUMNS = 80;

    private static final int SCREEN_ROWS = 24;

    private static final long DRAIN_HORIZON_MILLIS = 30_000L;

    private static final int DRAIN_STEP_LIMIT = 500;

    private static final long SECONDS_AGO_OF_THE_ALREADY_REPLIED_CALL = 900L;

    private static final long SECONDS_AGO_OF_THE_ALREADY_RECORDED_REPLY = 800L;

    private static final long SECONDS_AGO_OF_THE_CALL_THIS_RENDER_SHOWS = 60L;

    private static final long SECONDS_AGO_OF_EVERY_OUT_TOKEN = 30L;

    static final List<CallToUserGateDecision> gateDecisions =
        Collections.synchronizedList(new ArrayList<>());

    static final class CallToUserGateDecision {

        final String sessionKey;

        final boolean callToUserTagScanned;

        CallToUserGateDecision(String sessionKey, boolean callToUserTagScanned) {
            this.sessionKey = sessionKey;
            this.callToUserTagScanned = callToUserTagScanned;
        }

        @Override
        public String toString() {
            return sessionKey + (callToUserTagScanned
                ? " (the call-to-user tag scan was performed)"
                : " (the call-to-user tag scan was declined)");
        }
    }

    @Implements(BackgroundOutputTagScanner.class)
    public static class GateRecordingOutputTagScannerShadow {

        @RealObject
        BackgroundOutputTagScanner realScanner;

        @Implementation
        protected void scan(String sessionKey, String transcript, boolean scanCallToUserTag) {
            gateDecisions.add(new CallToUserGateDecision(sessionKey, scanCallToUserTag));
            Shadow.directlyOn(realScanner, BackgroundOutputTagScanner.class, "scan",
                ClassParameter.from(String.class, sessionKey),
                ClassParameter.from(String.class, transcript),
                ClassParameter.from(boolean.class, scanCallToUserTag));
        }
    }

    private TermuxActivity activity;

    private TermuxTerminalSessionActivityClient sessionActivityClient;

    private TermuxShellManager shellManager;

    private TerminalSession displayedSession;

    private String callTokenThisRenderShows;

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
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            sessionActivityClient);
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        activity.setContentView(R.layout.activity_termux);

        displayedSession = addDisplayedSessionHoldingARepliedStatusline();

        set(activity, TermuxActivity.class, "mIsVisible", true);

        drainEveryLooperToTheHorizon();
        storeTheRepliedStatuslineOfTheCyclePrecedingThisRender();
        gateDecisions.clear();
    }

    @Test
    public void periodicScanScansTheCallToUserTagWhenThisRenderIsWhatMakesTheCallUnreplied() {
        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        assertTrue("the fixture must begin from a stored statusline the owner has already replied to, so "
                + "a gate consulting the previous cycle declines the scan; without that this test could "
                + "pass without the gate ever having to consult the render under test",
            !store.shouldScanCallToUserTag(SESSION_NAME));

        renderAStatuslineWhoseCallIsAsNewAsItsReply(displayedSession);

        runOnePeriodicDisplayedSessionScan();
        drainEveryLooperToTheHorizon();

        assertEquals("the scan must have applied the call time this render shows, otherwise the render "
                + "under test never reached the store and the gate assertion below would be vacuous",
            callTokenThisRenderShows, clockToken(store.getStatuslineCallTimeMillis(SESSION_NAME)));

        assertEquals("the call-to-user gate decides whether the transcript reason scan runs on this tick, "
                + "and it must be read from the statusline this same render produced rather than from the "
                + "previous cycle's stored values; this render is what first makes the call time equal to "
                + "the reply time, so the scan has to be performed on this tick, otherwise the owner learns "
                + "which session is calling him a whole scan cycle later than the session asked, which is "
                + "the opposite of what making the scan cheaper is for; the decisions recorded for this "
                + "scan were " + gateDecisions,
            Collections.singletonList(true), callToUserTagScanDecisionsForTheSessionUnderTest());
    }

    private List<Boolean> callToUserTagScanDecisionsForTheSessionUnderTest() {
        List<Boolean> decisions = new ArrayList<>();
        for (CallToUserGateDecision decision : new ArrayList<>(gateDecisions)) {
            if (decision.sessionKey == null) continue;
            if (!decision.sessionKey.equals(displayedSession.mHandle)) continue;
            decisions.add(decision.callToUserTagScanned);
        }
        return decisions;
    }

    private void storeTheRepliedStatuslineOfTheCyclePrecedingThisRender() {
        SessionNewActivityStore store = activity.getSessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME,
            millisSecondsAgo(SECONDS_AGO_OF_THE_ALREADY_REPLIED_CALL),
            millisSecondsAgo(SECONDS_AGO_OF_EVERY_OUT_TOKEN),
            millisSecondsAgo(SECONDS_AGO_OF_THE_ALREADY_RECORDED_REPLY));
    }

    private void runOnePeriodicDisplayedSessionScan() {
        sessionActivityClient.startDisplayedSessionCallScanTick();
        sessionActivityClient.stopDisplayedSessionCallScanTick();
    }

    private TerminalSession addDisplayedSessionHoldingARepliedStatusline() throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], TRANSCRIPT_ROWS,
                activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = SESSION_NAME;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        shellManager.mTermuxSessions.add(
            constructor.newInstance(terminalSession, new ExecutionCommand(), null, false));
        OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
            () -> terminalSession.initializeEmulator(SCREEN_COLUMNS, SCREEN_ROWS, 10, 20));
        Field shellProcessId = TerminalSession.class.getDeclaredField("mShellPid");
        shellProcessId.setAccessible(true);
        shellProcessId.setInt(terminalSession, LIVE_SHELL_PROCESS_ID);
        appendStatusline(terminalSession,
            clockTokenSecondsAgo(SECONDS_AGO_OF_THE_ALREADY_REPLIED_CALL),
            clockTokenSecondsAgo(SECONDS_AGO_OF_THE_ALREADY_RECORDED_REPLY));
        return terminalSession;
    }

    private void renderAStatuslineWhoseCallIsAsNewAsItsReply(TerminalSession session) {
        callTokenThisRenderShows = clockTokenSecondsAgo(SECONDS_AGO_OF_THE_CALL_THIS_RENDER_SHOWS);
        appendStatusline(session, callTokenThisRenderShows, callTokenThisRenderShows);
    }

    private void appendStatusline(TerminalSession session, String callToken, String replyToken) {
        String rendered = "claude  call:" + callToken
            + "  out:" + clockTokenSecondsAgo(SECONDS_AGO_OF_EVERY_OUT_TOKEN)
            + "  reply:" + replyToken + "\r\n";
        byte[] renderedBytes = rendered.getBytes(StandardCharsets.UTF_8);
        session.getEmulator().append(renderedBytes, renderedBytes.length);
    }

    private long millisSecondsAgo(long secondsAgo) {
        return System.currentTimeMillis() - secondsAgo * 1_000L;
    }

    private String clockTokenSecondsAgo(long secondsAgo) {
        return clockToken(millisSecondsAgo(secondsAgo));
    }

    private String clockToken(Long timeMillis) {
        if (timeMillis == null) return "absent";
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(timeMillis);
        return String.format(Locale.US, "%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    private void drainEveryLooperToTheHorizon() {
        long horizonUptimeMillis = SystemClock.uptimeMillis() + DRAIN_HORIZON_MILLIS;
        for (int step = 0; step < DRAIN_STEP_LIMIT; step++) {
            for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
                OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(looper).idle());
            }
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(
                () -> shadowOf(Looper.getMainLooper()).idle());
            if (!everyLooperTheClientOwnsIsIdle()) continue;
            long nextTaskDueUptimeMillis =
                shadowOf(Looper.getMainLooper()).getNextScheduledTaskTime().toMillis();
            if (nextTaskDueUptimeMillis <= 0L) return;
            if (nextTaskDueUptimeMillis > horizonUptimeMillis) return;
            long remainingMillis = Math.max(0L, nextTaskDueUptimeMillis - SystemClock.uptimeMillis());
            long millisToAdvance = remainingMillis;
            OffDeviceNativeSubprocessLibrary.tolerateItsAbsence(() -> shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(millisToAdvance)));
        }
    }

    private boolean everyLooperTheClientOwnsIsIdle() {
        for (Looper looper : loopersTheClientOwnsBesidesTheMainOne()) {
            if (!shadowOf(looper).isIdle()) return false;
        }
        return shadowOf(Looper.getMainLooper()).isIdle();
    }

    private List<Looper> loopersTheClientOwnsBesidesTheMainOne() {
        List<Looper> loopers = new ArrayList<>();
        for (Field field : TermuxTerminalSessionActivityClient.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(sessionActivityClient);
            } catch (ReflectiveOperationException theFieldIsNotReadable) {
                continue;
            }
            Looper looper = null;
            if (value instanceof HandlerThread) looper = ((HandlerThread) value).getLooper();
            if (value instanceof Handler) looper = ((Handler) value).getLooper();
            if (looper == null) continue;
            if (looper == Looper.getMainLooper()) continue;
            if (!loopers.contains(looper)) loopers.add(looper);
        }
        return loopers;
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
