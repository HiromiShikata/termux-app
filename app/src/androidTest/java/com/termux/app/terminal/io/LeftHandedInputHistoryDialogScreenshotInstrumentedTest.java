package com.termux.app.terminal.io;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.handedness.Handedness;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class LeftHandedInputHistoryDialogScreenshotInstrumentedTest {

    private static final String PINNED_ENTRY = "pinned-history-entry";
    private static final String UNPINNED_ENTRY = "unpinned-history-entry";
    private static final long SETTLE_MILLIS = 700L;

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void inputHistoryDialogIsCapturedOffAndOnWithItsRowPlacementAsserted() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setPinnedTextInputHistory(
            new PinnedTextInputHistorySerializer().serialize(Collections.singletonList(PINNED_ENTRY)));
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            scenario.onActivity(activity -> {
                TerminalToolbarViewPager.PageAdapter pageAdapter = activity.getTerminalToolbarViewPagerAdapter();
                Assert.assertNotNull(pageAdapter);
                pageAdapter.addSubmittedTextInputToHistory(UNPINNED_ENTRY);
                pageAdapter.showSubmittedTextInputHistory(activity.getTerminalToolbarTextInput());
            });
            settle();
            assertRowsPlacedFor(scenario, Handedness.RIGHT_HANDED);
            captureDevice("left-handed-history-before.png");

            preferences.setLeftHandedModeEnabled(true);
            settle();
            assertRowsPlacedFor(scenario, Handedness.LEFT_HANDED);
            captureDevice("left-handed-history-after.png");
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            settle();
        } finally {
            preferences.setLeftHandedModeEnabled(false);
            preferences.setPinnedTextInputHistory(
                new PinnedTextInputHistorySerializer().serialize(Collections.emptyList()));
        }
    }

    private static void assertRowsPlacedFor(ActivityScenario<TermuxActivity> scenario, Handedness handedness) {
        scenario.onActivity(activity -> {
            SubmittedTextInputHistory history = new SubmittedTextInputHistory(5, Collections.singletonList(PINNED_ENTRY));
            history.add(UNPINNED_ENTRY);
            TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter adapter =
                new TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter(
                    activity, handedness, history, () -> { }, entry -> { });
            ViewGroup pinnedRow = (ViewGroup) adapter.getView(0, null, new android.widget.FrameLayout(activity));
            int restoreId = R.id.toolbar_text_input_history_restore_button;
            int pinId = R.id.toolbar_text_input_history_pin_button;
            if (handedness == Handedness.RIGHT_HANDED) {
                Assert.assertEquals("right-handed: restore button at the row's right end", restoreId,
                    pinnedRow.getChildAt(pinnedRow.getChildCount() - 1).getId());
                Assert.assertEquals("right-handed: pin button at the row's left end", pinId,
                    pinnedRow.getChildAt(0).getId());
            } else {
                Assert.assertEquals("left-handed: restore button at the row's left end", restoreId,
                    pinnedRow.getChildAt(0).getId());
                Assert.assertEquals("left-handed: pin button at the row's right end", pinId,
                    pinnedRow.getChildAt(pinnedRow.getChildCount() - 1).getId());
            }
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
        Log.i("LeftHandedToggle", "device capture " + fileName + " " + bitmap.getWidth() + "x" + bitmap.getHeight());
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
