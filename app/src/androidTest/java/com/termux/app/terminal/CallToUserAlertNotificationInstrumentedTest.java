package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.service.notification.StatusBarNotification;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.diagnostics.SessionCreationPath;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@RunWith(AndroidJUnit4.class)
public class CallToUserAlertNotificationInstrumentedTest {

    private static final String CALL_OPEN_TAG = "<" + "call-to-user" + ">";
    private static final String CALL_CLOSE_TAG = "</" + "call-to-user" + ">";
    private static final long WAIT_MILLIS = 20_000L;
    private static final String TOAST_TEXT = "calling session has ended";
    private static final String SHADE_SCREENSHOT_NAME = "call-alert-shade-notification.png";
    private static final String TAPPED_SCREENSHOT_NAME = "call-alert-tapped-session-a.png";

    private Instrumentation mInstrumentation;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private ActivityScenario<TermuxActivity> mScenario;
    private TermuxService mService;
    private TerminalSession mSessionA;
    private TerminalSession mSessionB;
    private String mNameA;
    private String mNameB;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = ApplicationProvider.getApplicationContext();
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences preferences = mContext.getSharedPreferences(
            CallToUserAlertNotifier.PREFERENCES_NAME, Context.MODE_PRIVATE);
        assertTrue(preferences.edit().clear().commit());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mNameA = "alert-a-" + suffix;
        mNameB = "alert-b-" + suffix;

        mScenario = ActivityScenario.launch(TermuxActivity.class);
        mService = waitFor(() -> serviceOf(mScenario));
        mSessionA = createSession(mNameA);
        mSessionB = createSession(mNameB);
        selectSession(mSessionB);
    }

    @After
    public void tearDown() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(CallToUserNotificationTags.tagFor(mNameA),
                CallToUserAlertNotifier.NOTIFICATION_ID);
            mNotificationManager.cancel(CallToUserNotificationTags.tagFor(mNameB),
                CallToUserAlertNotifier.NOTIFICATION_ID);
        }
        if (mScenario != null) {
            mScenario.close();
        }
    }

    @Test
    public void newCallPostsOneHighImportanceNotificationWithTheSessionNameAndCallText() throws Exception {
        recordCall(mSessionA, "Need approval for the release");

        StatusBarNotification notification = waitForAlert(mNameA, "Need approval for the release");

        assertEquals(1, alertsFor(mNameA).size());
        Notification built = notification.getNotification();
        assertEquals(CallToUserAlertNotifier.CHANNEL_ID, built.getChannelId());
        assertEquals(mNameA, built.extras.getCharSequence(Notification.EXTRA_TITLE).toString());
        assertEquals("Need approval for the release",
            built.extras.getCharSequence(Notification.EXTRA_TEXT).toString());
        assertEquals(NotificationManager.IMPORTANCE_HIGH,
            mNotificationManager.getNotificationChannel(CallToUserAlertNotifier.CHANNEL_ID).getImportance());
        captureShade();
    }

    @Test
    public void sameCallRedetectedAfterRestartIsNotPostedAgain() throws Exception {
        recordCall(mSessionA, "Same call");
        long firstPostTime = waitForAlert(mNameA, "Same call").getPostTime();

        rescanTranscript(mSessionA, "Same call");
        reloadLedgerAsAfterRestart();
        rescanTranscript(mSessionA, "Same call");

        List<StatusBarNotification> alerts = alertsFor(mNameA);
        assertEquals(1, alerts.size());
        assertEquals(firstPostTime, alerts.get(0).getPostTime());
    }

    @Test
    public void newCallInSameSessionReplacesAndRepostsTheNotification() throws Exception {
        recordCall(mSessionA, "First call");
        long firstPostTime = waitForAlert(mNameA, "First call").getPostTime();

        rescanTranscript(mSessionA, "Second call");

        StatusBarNotification replaced = waitForAlert(mNameA, "Second call");
        assertEquals(1, alertsFor(mNameA).size());
        assertTrue(replaced.getPostTime() >= firstPostTime);
    }

    @Test
    public void tapOpensTheCallingSessionWhileAnotherSessionIsDisplayed() throws Exception {
        recordCall(mSessionA, "Tap me");
        StatusBarNotification notification = waitForAlert(mNameA, "Tap me");
        assertEquals(mSessionB, currentSessionOf(mScenario));

        TermuxActivity tapped = sendIntentAndAwaitActivity(notification.getNotification().contentIntent);

        waitUntil(() -> currentSessionOf(tapped) == mSessionA);
        assertEquals(mSessionA, currentSessionOf(tapped));
        captureTapped();
    }

    @Test
    public void tapForAnEndedSessionShowsTheToastAndKeepsTheCurrentSession() throws Exception {
        TerminalSession endedSession = createSession("ended-" + mNameA);
        recordCall(endedSession, "Ended call");
        PendingIntent contentIntent = waitForAlert(endedSession.mSessionName, "Ended call")
            .getNotification().contentIntent;
        removeSession(endedSession);
        waitUntil(() -> alertsFor(endedSession.mSessionName).isEmpty());

        TermuxActivity tapped = sendIntentAndAwaitActivity(contentIntent);

        UiDevice device = UiDevice.getInstance(mInstrumentation);
        assertNotNull("the ended-session toast must be shown",
            device.wait(Until.hasObject(By.textContains(TOAST_TEXT)), WAIT_MILLIS));
        assertEquals(mSessionB, currentSessionOf(tapped));
    }

    @Test
    public void answeringTheCallRemovesTheSessionNotification() throws Exception {
        recordCall(mSessionA, "Answer me");
        waitForAlert(mNameA, "Answer me");

        acknowledgeThroughReplyPath(mNameA);

        waitUntil(() -> alertsFor(mNameA).isEmpty());
        assertEquals(0, alertsFor(mNameA).size());
    }

    private static TermuxService serviceOf(ActivityScenario<TermuxActivity> scenario) {
        final TermuxService[] holder = new TermuxService[1];
        scenario.onActivity(activity -> holder[0] = activity.getTermuxService());
        return holder[0];
    }

    private TerminalSession createSession(String name) throws Exception {
        final TermuxSession[] holder = new TermuxSession[1];
        mInstrumentation.runOnMainSync(() -> holder[0] = mService.createTermuxSession("/system/bin/sh",
            new String[0], null, mContext.getFilesDir().getAbsolutePath(), false, name,
            SessionCreationPath.NEW_SESSION_THE_OWNER_ASKED_FOR));
        assertNotNull(holder[0]);
        TerminalSession session = holder[0].getTerminalSession();
        assertEquals(name, session.mSessionName);
        byte[] echo = ("echo " + name + "\n").getBytes(StandardCharsets.UTF_8);
        session.write(echo, 0, echo.length);
        return session;
    }

    private void recordCall(TerminalSession session, String callText) {
        String handle = session.mHandle;
        String transcript = CALL_OPEN_TAG + callText + CALL_CLOSE_TAG;
        mInstrumentation.runOnMainSync(() -> mService.getCallToUserTagController()
            .onSessionTextChanged(handle, transcript));
    }

    private void rescanTranscript(TerminalSession session, String callText) {
        String handle = session.mHandle;
        String transcript = CALL_OPEN_TAG + callText + CALL_CLOSE_TAG;
        mInstrumentation.runOnMainSync(() -> {
            mService.getCallToUserTagController().forgetSession(handle);
            mService.getCallToUserTagController().onSessionTextChanged(handle, transcript);
        });
    }

    private void reloadLedgerAsAfterRestart() throws Exception {
        CallToUserAlertLedger restartedLedger = new CallToUserAlertLedger(
            CallToUserAlertNotifier.ledgerStorage(mContext));
        Field field = TermuxService.class.getDeclaredField("mCallToUserAlertLedger");
        field.setAccessible(true);
        mInstrumentation.runOnMainSync(() -> {
            try {
                field.set(mService, restartedLedger);
            } catch (IllegalAccessException illegalAccessException) {
                throw new RuntimeException(illegalAccessException);
            }
        });
    }

    private void acknowledgeThroughReplyPath(String sessionName) {
        long now = System.currentTimeMillis();
        mInstrumentation.runOnMainSync(() -> mService.getSessionNewActivityStore()
            .recordGenuineAppReply(sessionName, now));
    }

    private void selectSession(TerminalSession session) {
        mScenario.onActivity(activity -> activity.getTermuxTerminalSessionClient().setCurrentSession(session));
    }

    private void removeSession(TerminalSession session) {
        mInstrumentation.runOnMainSync(() -> {
            mService.removeTermuxSession(session);
            mService.pruneSessionNewActivityStoreToLiveSessions();
        });
    }

    private TermuxActivity sendIntentAndAwaitActivity(PendingIntent contentIntent) throws Exception {
        Instrumentation.ActivityMonitor monitor = mInstrumentation.addMonitor(TermuxActivity.class.getName(),
            null, false);
        contentIntent.send();
        TermuxActivity activity = (TermuxActivity) mInstrumentation.waitForMonitorWithTimeout(monitor, WAIT_MILLIS);
        assertNotNull("tapping the notification must open the app", activity);
        waitUntil(() -> serviceOf(activity) != null);
        return activity;
    }

    private StatusBarNotification waitForAlert(String sessionName, String callText) throws Exception {
        waitUntil(() -> {
            for (StatusBarNotification active : alertsFor(sessionName)) {
                if (callText.equals(active.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT)
                    .toString())) {
                    return true;
                }
            }
            return false;
        });
        for (StatusBarNotification active : alertsFor(sessionName)) {
            if (callText.equals(active.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT)
                .toString())) {
                return active;
            }
        }
        throw new AssertionError("alert with the call text disappeared");
    }

    private List<StatusBarNotification> alertsFor(String sessionName) {
        List<StatusBarNotification> alerts = new ArrayList<>();
        String tag = CallToUserNotificationTags.tagFor(sessionName);
        for (StatusBarNotification active : mNotificationManager.getActiveNotifications()) {
            if (tag.equals(active.getTag()) && active.getId() == CallToUserAlertNotifier.NOTIFICATION_ID) {
                alerts.add(active);
            }
        }
        return alerts;
    }

    private void captureShade() throws Exception {
        UiDevice device = UiDevice.getInstance(mInstrumentation);
        assertTrue(device.openNotification());
        assertNotNull("the shade must list the call notification",
            device.wait(Until.hasObject(By.text(mNameA)), WAIT_MILLIS));
        File file = screenshotFile(SHADE_SCREENSHOT_NAME);
        assertTrue(device.takeScreenshot(file));
        assertTrue(file.length() > 0);
        device.pressBack();
    }

    private void captureTapped() throws Exception {
        UiDevice device = UiDevice.getInstance(mInstrumentation);
        device.waitForIdle();
        File file = screenshotFile(TAPPED_SCREENSHOT_NAME);
        assertTrue(device.takeScreenshot(file));
        assertTrue(file.length() > 0);
    }

    private static File screenshotFile(String name) {
        File directory = new File(Environment.getExternalStorageDirectory(), "termux-instrumentation-screenshots");
        assertTrue(directory.exists() || directory.mkdirs());
        return new File(directory, name);
    }

    private TerminalSession currentSessionOf(ActivityScenario<TermuxActivity> scenario) {
        final TerminalSession[] holder = new TerminalSession[1];
        scenario.onActivity(activity -> holder[0] = activity.getCurrentSession());
        return holder[0];
    }

    private TerminalSession currentSessionOf(TermuxActivity activity) throws Exception {
        final TerminalSession[] holder = new TerminalSession[1];
        mInstrumentation.runOnMainSync(() -> holder[0] = activity.getCurrentSession());
        return holder[0];
    }

    private static TermuxService serviceOf(TermuxActivity activity) {
        return activity.getTermuxService();
    }

    private static <T> T waitFor(Callable<T> supplier) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        T value = supplier.call();
        while (value == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100L);
            value = supplier.call();
        }
        assertNotNull("timed out waiting for the value", value);
        return value;
    }

    private static void waitUntil(Callable<Boolean> condition) throws Exception {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (!Boolean.TRUE.equals(condition.call())) {
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("timed out waiting for the condition");
            }
            Thread.sleep(100L);
        }
    }
}
