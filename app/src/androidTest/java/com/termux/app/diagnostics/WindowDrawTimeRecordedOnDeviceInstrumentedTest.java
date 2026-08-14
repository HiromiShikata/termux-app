package com.termux.app.diagnostics;

import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

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
    public void theWindowDrawTimeRecordsADrawThatHappensAfterTheActivityIsLaunched() {
        long launchStartedAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();

        try (ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class)) {
            awaitDrawRecordedAfter(launchStartedAtElapsedRealtimeMillis);

            long readAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();
            DiagnosticsDrawTime drawTime =
                WindowDrawTimeRecorderHolder.getInstance().snapshot(readAtElapsedRealtimeMillis);

            assertTrue("the reading states how long ago the activity window last completed a draw"
                    + " pass, so an on-draw listener that never fires makes every reading say the"
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
    }

    private static long lastDrawAtElapsedRealtimeMillis(long readAtElapsedRealtimeMillis,
                                                        DiagnosticsDrawTime drawTime) {
        return readAtElapsedRealtimeMillis - drawTime.getMillisSinceLastDraw();
    }

    private static void awaitDrawRecordedAfter(long launchStartedAtElapsedRealtimeMillis) {
        long deadlineMillis = SystemClock.elapsedRealtime() + DRAW_RECORD_TIMEOUT_MILLIS;
        while (SystemClock.elapsedRealtime() < deadlineMillis) {
            long readAtElapsedRealtimeMillis = SystemClock.elapsedRealtime();
            DiagnosticsDrawTime drawTime =
                WindowDrawTimeRecorderHolder.getInstance().snapshot(readAtElapsedRealtimeMillis);
            if (drawTime.hasDrawn()
                    && lastDrawAtElapsedRealtimeMillis(readAtElapsedRealtimeMillis, drawTime)
                        >= launchStartedAtElapsedRealtimeMillis) {
                return;
            }
            SystemClock.sleep(DRAW_RECORD_POLL_INTERVAL_MILLIS);
        }
    }
}
