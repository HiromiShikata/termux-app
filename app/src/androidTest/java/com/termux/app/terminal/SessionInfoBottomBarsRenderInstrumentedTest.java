package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;
import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SessionInfoBottomBarsRenderInstrumentedTest {

    private static final String SESSION_NAME = "demo-long-session-name-0001";
    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long SERVICE_READY_TIMEOUT_MILLIS = 30_000L;
    private static final String FIRST_REASON = "sample pending message one";
    private static final String SECOND_REASON = "sample pending message two";
    private static final String EXPECTED_TIMES = "call: 3h  out: 12m  reply: 45s";
    private static final String EXPECTED_SCENE = SECOND_REASON;

    @Test
    public void productionBinderRendersSceneAlongsideTimesInTheCurrentSessionInfoArea() throws Exception {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);

        waitForServiceConnected(scenario);

        AtomicReference<File> writtenFileRef = new AtomicReference<>();
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            assertNotNull(client);
            client.stopActiveSessionSeenTick();

            SessionNewActivityStore store = new SessionNewActivityStore();
            long now = System.currentTimeMillis();
            store.recordStatuslineTimes(SESSION_NAME, now - 3L * ONE_HOUR_MILLIS,
                now - 12L * ONE_MINUTE_MILLIS, now - 45L * ONE_SECOND_MILLIS);
            store.recordExplicitCall(SESSION_NAME, now, FIRST_REASON);
            store.recordExplicitCall(SESSION_NAME, now, SECOND_REASON);

            View activityRoot = activity.findViewById(android.R.id.content);
            assertNotNull(activityRoot);
            SessionInfoBottomBarsBinder.bind(activityRoot, store, SESSION_NAME, now, () -> {
            });

            View bottomContainer = activity.findViewById(R.id.session_info_bottom_container);
            TextView timesBar = activity.findViewById(R.id.session_last_reply_bar);
            View sceneBar = activity.findViewById(R.id.session_pending_call_to_user_bar);
            TextView sceneText = activity.findViewById(R.id.session_pending_call_to_user_text);
            assertNotNull(bottomContainer);
            assertNotNull(timesBar);
            assertNotNull(sceneBar);
            assertNotNull(sceneText);

            assertEquals(View.VISIBLE, timesBar.getVisibility());
            assertEquals(View.VISIBLE, sceneBar.getVisibility());
            assertEquals(EXPECTED_TIMES, timesBar.getText().toString());
            assertEquals("the current-session info area must show only the single most recent "
                + "call-to-user message, not a pile-up of every unacknowledged reason",
                EXPECTED_SCENE, sceneText.getText().toString());

            Bitmap infoAreaBitmap = drawLaidOutViewToBitmap(bottomContainer);
            assertTrue(infoAreaBitmap.getWidth() > 0 && infoAreaBitmap.getHeight() > 0);
            File directory = activity.getExternalFilesDir(null);
            assertNotNull(directory);
            File output = new File(directory, "session-info-area-scene-render.png");
            writePngUnchecked(infoAreaBitmap, output);
            writtenFileRef.set(output);
        });

        File written = writtenFileRef.get();
        assertNotNull("rendered info-area PNG must be written", written);
        assertTrue("rendered screenshot file must exist and be non-empty",
            written.exists() && written.length() > 0L);
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
