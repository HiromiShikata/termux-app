package com.termux.app.browser;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class LeftHandedBrowserScreenshotInstrumentedTest {

    private static final long BROWSER_SETTLE_MILLIS = 3000L;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void browserTabBarAndProjectActionsAreCapturedOffAndOnWithTheirEdgeAsserted() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            scenario.onActivity(activity -> {
                TermuxBrowserController controller = activity.getTermuxBrowserController();
                Assert.assertTrue("a browser tab must open", controller.openUrlInNewTab("https://example.com/"));
                controller.showBrowser();
            });
            SystemClock.sleep(BROWSER_SETTLE_MILLIS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertProjectActionsEdge(scenario, false);
            captureDevice("left-handed-browser-before.png");

            preferences.setLeftHandedModeEnabled(true);
            SystemClock.sleep(BROWSER_SETTLE_MILLIS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertProjectActionsEdge(scenario, true);
            captureDevice("left-handed-browser-after.png");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static void assertProjectActionsEdge(ActivityScenario<TermuxActivity> scenario, boolean leftHanded) {
        scenario.onActivity(activity -> {
            View actions = activity.findViewById(R.id.browser_project_overview_actions);
            Assert.assertNotNull(actions);
            int horizontal = ((FrameLayout.LayoutParams) actions.getLayoutParams()).gravity
                & android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
            Log.i("LeftHandedToggle", "browser project actions visibility=" + actions.getVisibility()
                + " horizontalGravity=" + horizontal + " leftHanded=" + leftHanded);
            Assert.assertEquals("the project actions must sit at the "
                + (leftHanded ? "left" : "right") + " end",
                leftHanded ? android.view.Gravity.START : android.view.Gravity.END, horizontal);
        });
    }

    private static void captureDevice(String fileName) throws Exception {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        Assert.assertNotNull("the device screenshot must be available", bitmap);
        File directory = new File(Environment.getExternalStorageDirectory(), "termux-instrumentation-screenshots");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("cannot create " + directory);
        }
        try (FileOutputStream stream = new FileOutputStream(new File(directory, fileName))) {
            Assert.assertTrue("cannot write " + fileName, bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(700L);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
