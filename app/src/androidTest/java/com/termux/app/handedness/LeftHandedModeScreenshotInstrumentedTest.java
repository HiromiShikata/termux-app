package com.termux.app.handedness;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeScreenshotInstrumentedTest {

    private static final String SCREENSHOT_DIRECTORY_NAME = "termux-instrumentation-screenshots";
    private static final long ANIMATION_SETTLE_MILLIS = 700L;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void leftHandedModeMovesSessionListAndTabDrawerAndIsCapturedOffAndOn() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        try {
            preferences.setLeftHandedModeEnabled(false);
            captureMainScreenStates("before", false);
            preferences.setLeftHandedModeEnabled(true);
            captureMainScreenStates("after", true);
            captureSettingsScreen("before");
            preferences.setLeftHandedModeEnabled(false);
            captureSettingsScreen("after");
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private void captureMainScreenStates(String state, boolean leftHanded) throws Exception {
        try (ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    writeScreenshot(activity.getWindow().getDecorView(),
                        "left-handed-main-" + state + ".png");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            scenario.onActivity(activity -> activity.getSessionListBottomSheetController().show());
            SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.session_list_bottom_sheet_title);
                LinearLayout titleRow = (LinearLayout) title.getParent();
                int expectedFirstId = leftHanded
                    ? R.id.session_list_bottom_sheet_hidden_toggle_button
                    : R.id.session_list_bottom_sheet_title;
                Assert.assertEquals("the session list title row must start with "
                    + (leftHanded ? "the hidden-sessions toggle" : "the title") + " when left-handed mode is "
                    + (leftHanded ? "on" : "off"), expectedFirstId, titleRow.getChildAt(0).getId());
                try {
                    writeScreenshot(activity.getWindow().getDecorView(),
                        "left-handed-session-list-" + state + ".png");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
                activity.getSessionListBottomSheetController().hide();
            });
            SystemClock.sleep(ANIMATION_SETTLE_MILLIS);

            scenario.onActivity(activity -> {
                int expectedEdge = leftHanded ? Gravity.LEFT : Gravity.RIGHT;
                Assert.assertEquals("the tab drawer must open from the "
                    + (leftHanded ? "left" : "right") + " edge",
                    expectedEdge, horizontalEdge(activity.getDrawer()));
                activity.getDrawer().openDrawer(TabDrawerEdge.currentGravity(activity.getDrawer()), false);
            });
            SystemClock.sleep(ANIMATION_SETTLE_MILLIS);
            scenario.onActivity(activity -> {
                try {
                    writeScreenshot(activity.getWindow().getDecorView(),
                        "left-handed-drawer-" + state + ".png");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
                activity.getDrawer().closeDrawers();
            });
        }
    }

    private void captureSettingsScreen(String state) throws Exception {
        try (ActivityScenario<SettingsActivity> scenario =
                 ActivityScenario.launch(SettingsActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    writeScreenshot(activity.getWindow().getDecorView(),
                        "left-handed-settings-" + state + ".png");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    private static int horizontalEdge(@NonNull DrawerLayout drawer) {
        return Gravity.getAbsoluteGravity(TabDrawerEdge.currentGravity(drawer), View.LAYOUT_DIRECTION_LTR)
            & Gravity.HORIZONTAL_GRAVITY_MASK;
    }

    private static void writeScreenshot(@NonNull View view, @NonNull String fileName) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File directory = new File(Environment.getExternalStorageDirectory(), SCREENSHOT_DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("cannot create " + directory);
        }
        try (FileOutputStream stream = new FileOutputStream(new File(directory, fileName))) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw new IllegalStateException("cannot write " + fileName);
            }
        }
    }
}
