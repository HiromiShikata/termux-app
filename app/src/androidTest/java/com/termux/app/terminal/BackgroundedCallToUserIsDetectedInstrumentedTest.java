package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.app.Instrumentation;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BackgroundedCallToUserIsDetectedInstrumentedTest {

    private static final String OTHER_SESSION_NAME = "backgrounded-other-0001";
    private static final String CURRENT_SESSION_NAME = "backgrounded-current-0002";
    private static final String REASON = "NEEDS APPROVAL WHILE BACKGROUNDED";
    private static final int REPLY_BEHIND_CALL_SECONDS = 120;
    private static final int SECONDS_PER_DAY = 24 * 3600;
    private static final long SERVICE_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long SESSION_READY_TIMEOUT_MILLIS = 15_000L;
    private static final long CALL_REASON_RECORDED_TIMEOUT_MILLIS = 20_000L;
    private static final long ACTIVITY_STOPPED_TIMEOUT_MILLIS = 20_000L;

    @Test
    public void aCallRaisedInANonCurrentSessionIsDetectedWhileTheActivityIsStopped() throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        waitForServiceConnected(scenario);

        AtomicReference<TerminalSession> otherSessionRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            client.addNewSession(true, OTHER_SESSION_NAME);
            otherSessionRef.set(activity.getCurrentSession());
            client.addNewSession(true, CURRENT_SESSION_NAME);
            client.stopActiveSessionSeenTick();
            client.stopAllSessionsCallScanTick();
            client.stopDisplayedSessionCallScanTick();
        });

        TerminalSession otherSession = waitForSessionEmulator(scenario, otherSessionRef);
        assertNotNull("the non-current session must exist with an initialized emulator", otherSession);
        assertEquals("the captured session must be the non-current one", OTHER_SESSION_NAME,
            otherSession.mSessionName);

        sendTheActivityToTheBackground(scenario);

        scenario.onActivity(activity -> {
            TerminalEmulator emulator = otherSession.getEmulator();
            assertNotNull(emulator);
            byte[] bytes = taggedOutputWithUnrepliedStatuslineCall().getBytes(StandardCharsets.UTF_8);
            emulator.append(bytes, bytes.length);

            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            client.forgetBackgroundOutputScanThrottle(otherSession);
            client.startDisplayedSessionCallScanTick();
        });

        waitForUnacknowledgedCallReason(scenario);

        AtomicReference<String> unacknowledgedRef = new AtomicReference<>("");
        AtomicReference<SessionNewActivityTier> tierRef =
            new AtomicReference<>(SessionNewActivityTier.NONE);
        scenario.onActivity(activity -> {
            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            unacknowledgedRef.set(String.join("|", store.getUnacknowledgedCallReasons(OTHER_SESSION_NAME)));
            tierRef.set(store.tierFor(OTHER_SESSION_NAME));
        });

        assertEquals("the periodic cycle must record the call raised in a session the owner is not "
                + "looking at while the app is backgrounded; leaving it undetected is what strands that "
                + "session and skews the order the sessions are worked in",
            REASON, unacknowledgedRef.get());
        assertEquals("that detected call must arm the red tier so the session surfaces to the owner",
            SessionNewActivityTier.RED, tierRef.get());
    }

    /**
     * Puts the app in the state the owner is actually in when a call goes unnoticed: the task sent to
     * the background, so the activity runs through onStop and reports itself as not visible.
     *
     * <p>{@link ActivityScenario#moveToState} cannot be used for this. TermuxActivity is declared with
     * {@code android:launchMode="singleTask"} in AndroidManifest.xml, and ActivityScenario does not
     * drive lifecycle transitions for a singleTask activity — the requested transition times out with
     * the activity still RESUMED. Moving the task to the back is the transition the platform itself
     * performs when the owner leaves the app, so it reaches the same state without the harness
     * limitation.
     */
    private static void sendTheActivityToTheBackground(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        scenario.onActivity(activity -> activity.moveTaskToBack(true));

        long deadline = System.currentTimeMillis() + ACTIVITY_STOPPED_TIMEOUT_MILLIS;
        AtomicBoolean stopped = new AtomicBoolean(false);
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> stopped.set(!activity.isVisible()));
            if (stopped.get()) break;
            Thread.sleep(100L);
        }

        scenario.onActivity(activity -> assertFalse("the activity must report itself as not visible so "
                + "this exercises the state the owner is actually in when a call goes unnoticed",
            activity.isVisible()));
    }

    private static String taggedOutputWithUnrepliedStatuslineCall() {
        int secondsSinceMidnight = secondsSinceMidnightNow();
        String callClock = clock(secondsSinceMidnight);
        String replyClock = clock(secondsSinceMidnight - REPLY_BEHIND_CALL_SECONDS);
        return "build finished\r\n"
            + "<call-to-user>" + REASON + "</call-to-user>\r\n"
            + "claude  call:" + callClock + "  out:" + callClock
            + "  reply:" + replyClock + "\r\n";
    }

    private static int secondsSinceMidnightNow() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        return calendar.get(Calendar.HOUR_OF_DAY) * 3600
            + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
    }

    private static String clock(int secondsSinceMidnight) {
        int wrapped = ((secondsSinceMidnight % SECONDS_PER_DAY) + SECONDS_PER_DAY) % SECONDS_PER_DAY;
        return String.format(Locale.US, "%02d:%02d:%02d",
            wrapped / 3600, (wrapped % 3600) / 60, wrapped % 60);
    }

    private static void waitForUnacknowledgedCallReason(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + CALL_REASON_RECORDED_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            AtomicBoolean recorded = new AtomicBoolean(false);
            scenario.onActivity(activity -> {
                SessionNewActivityStore store = activity.getSessionNewActivityStore();
                recorded.set(store != null
                    && !store.getUnacknowledgedCallReasons(OTHER_SESSION_NAME).isEmpty());
            });
            if (recorded.get()) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(100L);
        }
        throw new AssertionError("the backgrounded periodic cycle did not record any unacknowledged "
            + "call reason for " + OTHER_SESSION_NAME + " within "
            + CALL_REASON_RECORDED_TIMEOUT_MILLIS + "ms");
    }

    private static void waitForServiceConnected(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SERVICE_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            AtomicBoolean ready = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                ready.set(activity.getTermuxService() != null
                    && activity.getTermuxTerminalSessionClient() != null));
            if (ready.get()) return;
            Thread.sleep(200L);
        }
        throw new AssertionError("the termux service did not connect within "
            + SERVICE_READY_TIMEOUT_MILLIS + "ms");
    }

    private static TerminalSession waitForSessionEmulator(ActivityScenario<TermuxActivity> scenario,
                                                          AtomicReference<TerminalSession> sessionRef)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SESSION_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            instrumentation.waitForIdleSync();
            TerminalSession session = sessionRef.get();
            if (session != null && session.getEmulator() != null) return session;
            Thread.sleep(100L);
        }
        return sessionRef.get();
    }
}
