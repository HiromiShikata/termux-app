package com.termux.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.diagnostics.ScrollbarViewCensus;
import com.termux.app.diagnostics.ScrollbarViewCensusSnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TermuxActivityWindowRegistrationInstrumentedTest {

    private static final long REGISTRATION_TIMEOUT_MILLIS = 10000L;

    private static final long REGISTRATION_POLL_INTERVAL_MILLIS = 50L;

    @Test
    public void theLaunchedActivityRegistersItsWindowSoTheScrollbarViewCensusWalksIt() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        View[] launchedDecorView = new View[1];
        scenario.onActivity(activity -> launchedDecorView[0] = activity.getWindow().getDecorView());
        assertNotNull("the census is read against the window this activity builds, so its decor view"
            + " is what the reading has to be checked for", launchedDecorView[0]);

        ScrollbarViewCensus census = awaitCensusOverAtLeastOneWindow();

        assertTrue("the report reads this census through the process-wide registry with no activity"
                + " in hand, so an activity that never registers its window root produces a reading"
                + " over zero windows that is indistinguishable from a census which found nothing,"
                + " and the pending scrollbar fade callbacks are then compared against that empty"
                + " count. Windows walked: " + census.getWindowCount()
                + ", windows no longer reachable: " + census.getWindowsNoLongerReachableCount(),
            census.getWindowCount() >= 1);
        assertTrue("a registry holding a root whose tree is never walked reports windows while"
                + " counting no view, and the terminal view of this window enables a vertical"
                + " scrollbar that fades, so a total of zero here means the walk did not reach it."
                + " Total: " + census.getScrollbarViewCount()
                + " over " + census.getWindowCount() + " window(s)",
            census.getScrollbarViewCount() >= 1);
    }

    private static ScrollbarViewCensus awaitCensusOverAtLeastOneWindow() {
        long deadlineMillis = SystemClock.elapsedRealtime() + REGISTRATION_TIMEOUT_MILLIS;
        ScrollbarViewCensus census = censusTakenOnTheMainThread();
        while (SystemClock.elapsedRealtime() < deadlineMillis) {
            if (census.getWindowCount() >= 1 && census.getScrollbarViewCount() >= 1) return census;
            SystemClock.sleep(REGISTRATION_POLL_INTERVAL_MILLIS);
            census = censusTakenOnTheMainThread();
        }
        return census;
    }

    private static ScrollbarViewCensus censusTakenOnTheMainThread() {
        ScrollbarViewCensus[] taken = new ScrollbarViewCensus[1];
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync(() -> taken[0] = ScrollbarViewCensusSnapshot.take());
        return taken[0];
    }
}
