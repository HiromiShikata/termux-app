package com.termux.app.handedness;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeScreenshotInstrumentedTest {

    private static final String SCREENSHOT_DIRECTORY_NAME = "termux-instrumentation-screenshots";
    private static final String LOG_TAG = "LeftHandedToggle";
    private static final long ANIMATION_SETTLE_MILLIS = 700L;
    private static final int MIN_TERMINAL_NON_BACKGROUND_PIXELS = 50;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void leftHandedModeMovesSessionListAndTabDrawerAndIsCapturedOffAndOn() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            scenario.onActivity(activity -> {
                TerminalSession session = activity.getTerminalView().getCurrentSession();
                Assert.assertNotNull("the terminal must have a session to show output", session);
                byte[] probe = "LEFT-HANDED-TERMINAL-PROBE\r\n".getBytes(StandardCharsets.UTF_8);
                session.getEmulator().append(probe, probe.length);
                activity.getTerminalView().invalidate();
            });
            settle();
            captureMainScreen(scenario, "before", false);
            captureSessionList(scenario, "before", false);
            captureDrawer(scenario, "before", false);

            preferences.setLeftHandedModeEnabled(true);
            settle();
            captureMainScreen(scenario, "after", true);
            captureSessionList(scenario, "after", true);
            captureDrawer(scenario, "after", true);

            captureSettingsScreen("after");
            preferences.setLeftHandedModeEnabled(false);
            captureSettingsScreen("before");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    @Test
    public void switchButtonCapturesRightHandedAfterTapAndLeftHandedScreenshots() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            captureSwitchButtonScreen(scenario, "handedness-switch-right-handed.png", "switch right-handed",
                false);

            scenario.onActivity(activity ->
                activity.findViewById(R.id.terminal_toolbar_handedness_switch_button).performClick());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            captureSwitchButtonScreen(scenario, "handedness-switch-after-tap.png", "switch after tap",
                preferences.isLeftHandedModeEnabled());

            settle();
            captureSwitchButtonScreen(scenario, "handedness-switch-left-handed.png", "switch left-handed",
                true);

            scenario.onActivity(activity ->
                activity.findViewById(R.id.terminal_toolbar_handedness_switch_button).performClick());
            settle();
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static void captureSwitchButtonScreen(ActivityScenario<TermuxActivity> scenario, String fileName,
                                                  String label, boolean leftHanded) {
        scenario.onActivity(activity -> {
            try {
                writeScreenshot(activity.getWindow().getDecorView(), fileName);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            Log.i(LOG_TAG, label + " screenshot=" + fileName + " leftHanded=" + leftHanded);
        });
    }

    private static void captureMainScreen(ActivityScenario<TermuxActivity> scenario, String state,
                                          boolean leftHanded) {
        scenario.onActivity(activity -> captureWithTerminalProbe(activity,
            "left-handed-main-" + state + ".png", "main " + state, state, leftHanded));
    }

    private static void captureSessionList(ActivityScenario<TermuxActivity> scenario, String state,
                                           boolean leftHanded) {
        scenario.onActivity(activity -> activity.getSessionListBottomSheetController().show());
        SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            TextView title = activity.findViewById(R.id.session_list_bottom_sheet_title);
            LinearLayout titleRow = (LinearLayout) title.getParent();
            int expectedFirstId = leftHanded
                ? R.id.session_list_bottom_sheet_hidden_toggle_button
                : R.id.session_list_bottom_sheet_title;
            Assert.assertEquals("the session list title row must start with the expected control when "
                + "left-handed mode is " + (leftHanded ? "on" : "off"),
                expectedFirstId, titleRow.getChildAt(0).getId());
            captureWithTerminalProbe(activity, "left-handed-session-list-" + state + ".png",
                "session list " + state, state, leftHanded);
        });
        scenario.onActivity(activity -> activity.getSessionListBottomSheetController().hide());
        SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void captureDrawer(ActivityScenario<TermuxActivity> scenario, String state,
                                      boolean leftHanded) {
        int expectedEdge = leftHanded ? Gravity.LEFT : Gravity.RIGHT;
        scenario.onActivity(activity -> {
            Assert.assertEquals("the tab drawer must open from the " + (leftHanded ? "left" : "right")
                + " edge", expectedEdge, horizontalEdge(activity.getDrawer()));
            activity.getDrawer().openDrawer(TabDrawerEdge.currentGravity(activity.getDrawer()), false);
        });
        SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            captureWithTerminalProbe(activity, "left-handed-drawer-" + state + ".png",
                "drawer " + state, state, leftHanded);
            activity.getDrawer().closeDrawers();
        });
        SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void captureWithTerminalProbe(TermuxActivity activity, String fileName, String label,
                                                 String state, boolean leftHanded) {
        View decor = activity.getWindow().getDecorView();
        Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(bitmap));
        writePng(bitmap, fileName);
        TerminalView terminal = activity.getTerminalView();
        int[] rect = windowRectOf(terminal);
        int count = countNonBackgroundPixels(bitmap, rect);
        Log.i(LOG_TAG, label + " terminal probe=" + count + " rect=" + rect[0] + "," + rect[1] + ","
            + rect[2] + "," + rect[3] + " leftHanded=" + leftHanded);
        if ("after".equals(state)) {
            Assert.assertTrue(label + " must show terminal text in the terminal area, probe=" + count,
                count > MIN_TERMINAL_NON_BACKGROUND_PIXELS);
        }
    }

    private static int[] windowRectOf(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new int[] {location[0], location[1], location[0] + view.getWidth(),
            location[1] + view.getHeight()};
    }

    private static int countNonBackgroundPixels(Bitmap bitmap, int[] rect) {
        int left = Math.max(0, rect[0]);
        int top = Math.max(0, rect[1]);
        int right = Math.min(bitmap.getWidth(), rect[2]);
        int bottom = Math.min(bitmap.getHeight(), rect[3]);
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) {
            return 0;
        }
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, left, top, width, height);
        Map<Integer, Integer> histogram = new HashMap<>();
        int mostCommonCount = 0;
        for (int pixel : pixels) {
            int count = histogram.merge(pixel, 1, Integer::sum);
            mostCommonCount = Math.max(mostCommonCount, count);
        }
        return pixels.length - mostCommonCount;
    }

    private void captureSettingsScreen(String state) {
        ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            try {
                writeScreenshot(activity.getWindow().getDecorView(), "left-handed-settings-" + state + ".png");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                activity.finish();
            }
        });
    }

    private static int horizontalEdge(androidx.drawerlayout.widget.DrawerLayout drawer) {
        return Gravity.getAbsoluteGravity(TabDrawerEdge.currentGravity(drawer), View.LAYOUT_DIRECTION_LTR)
            & Gravity.HORIZONTAL_GRAVITY_MASK;
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void writeScreenshot(@NonNull View view, @NonNull String fileName) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        writePng(bitmap, fileName);
    }

    private static void writePng(@NonNull Bitmap bitmap, @NonNull String fileName) {
        try {
            File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY_NAME);
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("cannot create " + directory);
            }
            try (FileOutputStream stream = new FileOutputStream(new File(directory, fileName))) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw new IllegalStateException("cannot write " + fileName);
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
