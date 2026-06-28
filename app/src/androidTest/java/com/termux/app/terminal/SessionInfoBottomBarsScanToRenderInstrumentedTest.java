package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SessionInfoBottomBarsScanToRenderInstrumentedTest {

    private static final String SESSION_NAME = "scan-render-session-0001";
    private static final String REASON = "NEEDS APPROVAL TO DEPLOY";
    private static final int REPLY_BEHIND_CALL_SECONDS = 120;
    private static final int SECONDS_PER_DAY = 24 * 3600;
    private static final long SERVICE_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long SESSION_READY_TIMEOUT_MILLIS = 15_000L;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

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

    @Test
    public void realScanToStoreToRenderPipelineSurfacesTheSceneInTheCurrentSessionInfoArea()
        throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);

        waitForServiceConnected(scenario);

        AtomicReference<TerminalSession> sessionRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            client.addNewSession(true, SESSION_NAME);
            client.stopActiveSessionSeenTick();
            sessionRef.set(activity.getCurrentSession());
        });

        TerminalSession session = waitForSessionEmulator(scenario, sessionRef);
        assertNotNull("a current session with an initialized emulator must exist", session);

        AtomicReference<File> writtenFileRef = new AtomicReference<>();
        AtomicReference<String> renderedSceneTextRef = new AtomicReference<>("");
        AtomicReference<Integer> sceneVisibilityRef = new AtomicReference<>(View.GONE);
        AtomicReference<SessionNewActivityTier> tierRef =
            new AtomicReference<>(SessionNewActivityTier.NONE);
        AtomicReference<String> unacknowledgedRef = new AtomicReference<>("");

        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);

            TerminalEmulator emulator = session.getEmulator();
            assertNotNull(emulator);
            byte[] bytes =
                taggedOutputWithUnrepliedStatuslineCall().getBytes(StandardCharsets.UTF_8);
            emulator.append(bytes, bytes.length);

            client.onTextChanged(session);

            SessionNewActivityStore store = activity.getSessionNewActivityStore();
            assertNotNull(store);
            tierRef.set(store.tierFor(SESSION_NAME));
            unacknowledgedRef.set(String.join("|", store.getUnacknowledgedCallReasons(SESSION_NAME)));

            View bottomContainer = activity.findViewById(R.id.session_info_bottom_container);
            View sceneBar = activity.findViewById(R.id.session_pending_call_to_user_bar);
            TextView sceneText = activity.findViewById(R.id.session_pending_call_to_user_text);
            assertNotNull(bottomContainer);
            assertNotNull(sceneBar);
            assertNotNull(sceneText);

            sceneVisibilityRef.set(sceneBar.getVisibility());
            renderedSceneTextRef.set(sceneText.getText().toString());

            Bitmap infoAreaBitmap = drawLaidOutViewToBitmap(bottomContainer);
            assertTrue(infoAreaBitmap.getWidth() > 0 && infoAreaBitmap.getHeight() > 0);
            File output = new File(sharedScreenshotDirectory(), "session-info-area-scan-render.png");
            writePngUnchecked(infoAreaBitmap, output);
            writtenFileRef.set(output);
        });

        System.out.println("SCAN_RENDER tier=" + tierRef.get()
            + " unacknowledged=[" + unacknowledgedRef.get() + "]"
            + " sceneVisible=" + (sceneVisibilityRef.get() == View.VISIBLE)
            + " sceneText=[" + renderedSceneTextRef.get() + "]");

        File written = writtenFileRef.get();
        assertNotNull("rendered info-area PNG must be written", written);
        assertTrue("rendered screenshot file must exist and be non-empty",
            written.exists() && written.length() > 0L);

        assertEquals("the real scan path must record the call as an unacknowledged reason",
            REASON, unacknowledgedRef.get());
        assertEquals("the real scan path must arm the red tier", SessionNewActivityTier.RED,
            tierRef.get());
        assertEquals("the scene bar must be visible after the real scan-to-render pipeline runs",
            View.VISIBLE, (int) sceneVisibilityRef.get());
        assertEquals("the scene text must render the recorded call reason", REASON,
            renderedSceneTextRef.get());
    }

    private static void waitForServiceConnected(ActivityScenario<TermuxActivity> scenario)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SERVICE_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean ready = new AtomicBoolean(false);
            scenario.onActivity(activity ->
                ready.set(activity.getTermuxService() != null
                    && activity.getSessionNewActivityStore() != null));
            if (ready.get()) {
                instrumentation.waitForIdleSync();
                return;
            }
            Thread.sleep(250L);
        }
        throw new AssertionError("TermuxService did not connect within timeout");
    }

    private static TerminalSession waitForSessionEmulator(ActivityScenario<TermuxActivity> scenario,
                                                          AtomicReference<TerminalSession> sessionRef)
        throws InterruptedException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        long deadline = System.currentTimeMillis() + SESSION_READY_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<TerminalSession> readyRef = new AtomicReference<>();
            scenario.onActivity(activity -> {
                TerminalSession current = activity.getCurrentSession();
                if (current != null && current.getEmulator() != null
                    && SESSION_NAME.equals(current.mSessionName)) {
                    readyRef.set(current);
                }
            });
            if (readyRef.get() != null) {
                instrumentation.waitForIdleSync();
                return readyRef.get();
            }
            Thread.sleep(250L);
        }
        return sessionRef.get();
    }

    private static File sharedScreenshotDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(),
            "termux-instrumentation-screenshots");
        if (!directory.exists()) {
            assertTrue("shared screenshot directory must be creatable so the rendered PNG survives "
                + "the post-test APK uninstall and can be pulled as a CI artifact", directory.mkdirs());
        }
        return directory;
    }

    private static Bitmap drawLaidOutViewToBitmap(View view) {
        int width = view.getWidth();
        assertTrue("info area must be laid out on device before capture", width > 0);
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, view.getMeasuredHeight());
        view.layout(view.getLeft(), view.getTop(), view.getLeft() + width, view.getTop() + height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF101010);
        view.draw(canvas);
        return bitmap;
    }

    private static void writePngUnchecked(Bitmap bitmap, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
