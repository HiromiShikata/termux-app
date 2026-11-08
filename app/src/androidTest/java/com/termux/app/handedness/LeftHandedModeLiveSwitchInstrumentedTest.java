package com.termux.app.handedness;

import android.Manifest;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.View;

import com.termux.R;
import com.termux.app.TermuxActivity;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeLiveSwitchInstrumentedTest {

    @Rule
    public GrantPermissionRule writeExternalStoragePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final long SETTLE_MILLIS = 700L;

    private static final class Layout {
        private TermuxActivity activity;
        private List<Integer> iconRowIds;
        private List<Integer> bottomContainerIds;
        private List<Integer> sessionInfoIds;
        private int iconRowGravity;
    }

    @Test
    public void togglingTheSettingWhileResumedMovesControlsAndRestoresThemInTheSameActivity()
        throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            Layout rightHanded = capture(scenario);

            preferences.setLeftHandedModeEnabled(true);
            settle();
            Layout leftHanded = capture(scenario);

            Assert.assertSame("the switch must apply without recreating the activity",
                rightHanded.activity, leftHanded.activity);
            assertReversed("icon button row", rightHanded.iconRowIds, leftHanded.iconRowIds);
            assertReversed("bottom container", rightHanded.bottomContainerIds, leftHanded.bottomContainerIds);
            assertReversed("session info bottom container", rightHanded.sessionInfoIds,
                leftHanded.sessionInfoIds);
            Assert.assertEquals("icon button row gravity must mirror the right-handed gravity",
                HandednessGravity.mirrored(rightHanded.iconRowGravity, Handedness.LEFT_HANDED),
                leftHanded.iconRowGravity);

            preferences.setLeftHandedModeEnabled(false);
            settle();
            Layout restored = capture(scenario);

            Assert.assertSame("the restore must also keep the same activity",
                rightHanded.activity, restored.activity);
            Assert.assertEquals("the right-handed icon order must come back",
                rightHanded.iconRowIds, restored.iconRowIds);
            Assert.assertEquals("the right-handed bottom order must come back",
                rightHanded.bottomContainerIds, restored.bottomContainerIds);
            Assert.assertEquals("the right-handed session info order must come back",
                rightHanded.sessionInfoIds, restored.sessionInfoIds);
            Assert.assertEquals("the right-handed icon gravity must come back",
                rightHanded.iconRowGravity, restored.iconRowGravity);
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    @Test
    public void switchButtonFlipsModeAndPlacementAndFlipsBackInTheSameActivity() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            SwitchButtonLayout rightHanded = captureSwitchButton(scenario);

            scenario.onActivity(activity ->
                activity.findViewById(R.id.terminal_toolbar_handedness_switch_button).performClick());
            settle();
            Assert.assertTrue("the switch button must turn left-handed mode on",
                preferences.isLeftHandedModeEnabled());
            SwitchButtonLayout leftHanded = captureSwitchButton(scenario);

            Assert.assertSame("the switch must apply without recreating the activity",
                rightHanded.activity, leftHanded.activity);
            assertReversed("bottom container", rightHanded.bottomContainerIds, leftHanded.bottomContainerIds);
            Assert.assertEquals("the switch button must sit at the right edge of the toolbar",
                leftHanded.containerWidth, leftHanded.buttonRight);

            scenario.onActivity(activity ->
                activity.findViewById(R.id.terminal_toolbar_handedness_switch_button).performClick());
            settle();
            Assert.assertFalse("the second tap must turn left-handed mode off",
                preferences.isLeftHandedModeEnabled());
            SwitchButtonLayout restored = captureSwitchButton(scenario);

            Assert.assertSame("the restore must also keep the same activity",
                rightHanded.activity, restored.activity);
            Assert.assertEquals("the right-handed bottom order must come back",
                rightHanded.bottomContainerIds, restored.bottomContainerIds);
            Assert.assertEquals("the switch button must return to the left edge of the toolbar",
                0, restored.buttonLeft);
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static final class SwitchButtonLayout {
        private TermuxActivity activity;
        private List<Integer> bottomContainerIds;
        private int containerWidth;
        private int buttonLeft;
        private int buttonRight;
    }

    private static SwitchButtonLayout captureSwitchButton(@NonNull ActivityScenario<TermuxActivity> scenario) {
        SwitchButtonLayout layout = new SwitchButtonLayout();
        scenario.onActivity(activity -> {
            LinearLayout bottom = activity.findViewById(R.id.terminal_toolbar_bottom_container);
            View button = activity.findViewById(R.id.terminal_toolbar_handedness_switch_button);
            layout.activity = activity;
            layout.bottomContainerIds = childIds(bottom);
            layout.containerWidth = bottom.getWidth();
            layout.buttonLeft = button.getLeft();
            layout.buttonRight = button.getRight();
        });
        return layout;
    }

    private static void assertReversed(String name, List<Integer> rightHandedIds, List<Integer> leftHandedIds) {
        List<Integer> expected = new ArrayList<>(rightHandedIds);
        Collections.reverse(expected);
        Assert.assertEquals("the " + name + " must be reversed when left-handed mode is on",
            expected, leftHandedIds);
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(SETTLE_MILLIS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static Layout capture(@NonNull ActivityScenario<TermuxActivity> scenario) {
        Layout layout = new Layout();
        scenario.onActivity(activity -> {
            layout.activity = activity;
            layout.iconRowIds = childIds(activity.findViewById(R.id.terminal_toolbar_icon_button_row));
            layout.bottomContainerIds =
                childIds(activity.findViewById(R.id.terminal_toolbar_bottom_container));
            layout.sessionInfoIds = childIds(activity.findViewById(R.id.session_info_bottom_container));
            layout.iconRowGravity =
                ((LinearLayout) activity.findViewById(R.id.terminal_toolbar_icon_button_row)).getGravity();
        });
        return layout;
    }

    private static List<Integer> childIds(@NonNull android.view.View container) {
        ViewGroup group = (ViewGroup) container;
        List<Integer> ids = new ArrayList<>();
        for (int index = 0; index < group.getChildCount(); index++) {
            ids.add(group.getChildAt(index).getId());
        }
        return ids;
    }

    private static final class TerminalProbe {
        private TerminalSession session;
        private boolean emulatorPresent;
        private int drawnNonBackgroundPixels;
    }

    @Test
    public void terminalKeepsItsSessionAndDrawnOutputAcrossAnImmediateOnAndOffToggle() throws Exception {
        TermuxAppSharedPreferences preferences =
            TermuxAppSharedPreferences.build(ApplicationProvider.getApplicationContext());
        preferences.setLeftHandedModeEnabled(false);
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        try {
            settle();
            scenario.onActivity(activity -> {
                TerminalSession session = activity.getTerminalView().getCurrentSession();
                Assert.assertNotNull("the terminal must have a session to show output", session);
                Assert.assertNotNull("the session must have an emulator", session.getEmulator());
                byte[] probe = "LEFT-HANDED-TERMINAL-PROBE\r\n".getBytes(StandardCharsets.UTF_8);
                session.getEmulator().append(probe, probe.length);
                activity.getTerminalView().invalidate();
            });
            settle();
            TerminalProbe before = probeTerminal(scenario);
            captureActivityDecor(scenario, "left-handed-toggle-main-before.png");

            preferences.setLeftHandedModeEnabled(true);
            settle();
            TerminalProbe on = probeTerminal(scenario);
            captureActivityDecor(scenario, "left-handed-toggle-main-after.png");

            preferences.setLeftHandedModeEnabled(false);
            settle();
            TerminalProbe off = probeTerminal(scenario);

            Assert.assertTrue("the terminal must draw text before the toggle, drawn="
                + before.drawnNonBackgroundPixels, before.drawnNonBackgroundPixels > 50);
            android.util.Log.i("LeftHandedToggle", "terminal probe: before=" + before.drawnNonBackgroundPixels
                + " on=" + on.drawnNonBackgroundPixels + " off=" + off.drawnNonBackgroundPixels
                + " sameSessionOn=" + (before.session == on.session)
                + " sameSessionOff=" + (before.session == off.session)
                + " emulatorOn=" + on.emulatorPresent + " emulatorOff=" + off.emulatorPresent);
            assertTerminalStillShows("ON", before, on);
            assertTerminalStillShows("OFF", before, off);
        } finally {
            preferences.setLeftHandedModeEnabled(false);
        }
    }

    private static void assertTerminalStillShows(String state, TerminalProbe before, TerminalProbe after) {
        Assert.assertSame("the same session must stay attached after switching " + state,
            before.session, after.session);
        Assert.assertTrue("the emulator must stay present after switching " + state, after.emulatorPresent);
        Assert.assertTrue("the drawn terminal text must stay comparable after switching " + state
                + ": before=" + before.drawnNonBackgroundPixels + " after=" + after.drawnNonBackgroundPixels,
            after.drawnNonBackgroundPixels >= before.drawnNonBackgroundPixels / 2
                && after.drawnNonBackgroundPixels > 0);
    }

    private static TerminalProbe probeTerminal(ActivityScenario<TermuxActivity> scenario) {
        TerminalProbe probe = new TerminalProbe();
        scenario.onActivity(activity -> {
            TerminalView view = activity.getTerminalView();
            probe.session = view.getCurrentSession();
            probe.emulatorPresent = probe.session != null && probe.session.getEmulator() != null;
            probe.drawnNonBackgroundPixels = countNonBackgroundPixels(view);
        });
        return probe;
    }

    private static int countNonBackgroundPixels(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        Map<Integer, Integer> histogram = new HashMap<>();
        int mostCommonCount = 0;
        for (int pixel : pixels) {
            int count = histogram.merge(pixel, 1, Integer::sum);
            mostCommonCount = Math.max(mostCommonCount, count);
        }
        return pixels.length - mostCommonCount;
    }

    private static void captureActivityDecor(ActivityScenario<TermuxActivity> scenario, String fileName) {
        scenario.onActivity(activity -> {
            try {
                View decor = activity.getWindow().getDecorView();
                Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
                decor.draw(new Canvas(bitmap));
                File directory = new File(Environment.getExternalStorageDirectory(), "termux-instrumentation-screenshots");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("cannot create " + directory);
                }
                try (FileOutputStream stream = new FileOutputStream(new File(directory, fileName))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
