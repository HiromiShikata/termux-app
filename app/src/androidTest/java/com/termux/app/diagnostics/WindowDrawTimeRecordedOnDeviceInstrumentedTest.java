package com.termux.app.diagnostics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WindowDrawTimeRecordedOnDeviceInstrumentedTest {

    private static final long DRAW_RECORD_TIMEOUT_MILLIS = 10000L;

    private static final long DRAW_RECORD_POLL_INTERVAL_MILLIS = 50L;

    @Test
    public void theWindowDrawTimeRecordsADrawByTheDecorViewOfTheLaunchedActivity() {
        long launchStartedAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();

        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        View[] launchedDecorView = new View[1];
        scenario.onActivity(activity -> launchedDecorView[0] = activity.getWindow().getDecorView());
        View decorView = launchedDecorView[0];
        assertNotNull("the reading names the window of this activity, so the decor view it draws"
            + " into is what the recorded draw has to be read against", decorView);
        awaitDrawRecordedAfter(decorView, launchStartedAtElapsedRealtimeMillis);

        long readAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();
        DiagnosticsDrawTime drawTime = WindowDrawTimeRecorderHolder.getInstance()
            .snapshot(decorView, readAtElapsedRealtimeMillis);

        assertTrue("the reading states how long ago the activity window last completed a draw"
                + " pass, so an on-draw listener that never fires, or one that records against"
                + " something other than the decor view it observes, makes every reading say the"
                + " window never drew while the window was in fact drawing",
            drawTime.hasDrawn());
        assertTrue("a draw recorded before this activity was launched would let one draw taken at"
                + " process start stand in for a window that has since stopped drawing, recorded "
                + drawTime.getMillisSinceLastDraw() + " ms before a read taken "
                + (readAtElapsedRealtimeMillis - launchStartedAtElapsedRealtimeMillis)
                + " ms after the launch began",
            lastDrawAtElapsedRealtimeMillis(readAtElapsedRealtimeMillis, drawTime)
                >= launchStartedAtElapsedRealtimeMillis);
    }

    private static long lastDrawAtElapsedRealtimeMillis(long readAtElapsedRealtimeMillis,
                                                        DiagnosticsDrawTime drawTime) {
        return readAtElapsedRealtimeMillis - drawTime.getMillisSinceLastDraw();
    }

    private static void awaitDrawRecordedAfter(View decorView,
                                               long launchStartedAtElapsedRealtimeMillis) {
        long deadlineMillis = SystemClock.elapsedRealtime() + DRAW_RECORD_TIMEOUT_MILLIS;
        while (SystemClock.elapsedRealtime() < deadlineMillis) {
            long readAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();
            DiagnosticsDrawTime drawTime = WindowDrawTimeRecorderHolder.getInstance()
                .snapshot(decorView, readAtElapsedRealtimeMillis);
            if (drawTime.hasDrawn()
                    && lastDrawAtElapsedRealtimeMillis(readAtElapsedRealtimeMillis, drawTime)
                        >= launchStartedAtElapsedRealtimeMillis) {
                return;
            }
            SystemClock.sleep(DRAW_RECORD_POLL_INTERVAL_MILLIS);
        }
    }
}
