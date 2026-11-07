package com.termux.app.handedness;

import android.os.SystemClock;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeDrawerEdgeInstrumentedTest {

    private static final long SETTLE_MILLIS = 600L;

    @Test
    public void rightHandedDrawerPathsOpenAndCloseTheEndDrawer() throws Exception {
        exerciseDrawerPaths(false, Gravity.END);
    }

    @Test
    public void leftHandedDrawerPathsOpenAndCloseTheStartDrawer() throws Exception {
        exerciseDrawerPaths(true, Gravity.START);
    }

    private static void exerciseDrawerPaths(boolean leftHanded, int expectedEdge) throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(leftHanded);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();

            scenario.onActivity(activity -> activity.getTermuxBrowserController().toggleTabsDrawer());
            settle();
            assertDrawerOpen(scenario, expectedEdge, true, "browser toggle must open the drawer");
            scenario.onActivity(activity -> activity.getTermuxBrowserController().toggleTabsDrawer());
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "browser toggle must close the drawer");

            scenario.onActivity(activity -> activity.getDrawer().openDrawer(expectedEdge, false));
            settle();
            scenario.onActivity(TermuxActivity::onBackPressed);
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "back must close an open drawer");

            scenario.onActivity(activity -> activity.getTermuxTerminalExtraKeys()
                .onTerminalExtraKeyButtonClick(null, "DRAWER", false, false, false, false));
            settle();
            assertDrawerOpen(scenario, expectedEdge, true, "the DRAWER extra key must open the drawer");
            scenario.onActivity(activity -> activity.getTermuxTerminalExtraKeys()
                .onTerminalExtraKeyButtonClick(null, "DRAWER", false, false, false, false));
            settle();
            assertDrawerOpen(scenario, expectedEdge, false, "the DRAWER extra key must close the drawer");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static void assertDrawerOpen(@NonNull ActivityScenario<TermuxActivity> scenario,
                                         int edge, boolean expectedOpen, String message) {
        scenario.onActivity(activity -> Assert.assertEquals(message + " at edge " + edge,
            expectedOpen, activity.getDrawer().isDrawerOpen(edge)));
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
