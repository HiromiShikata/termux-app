package com.termux.app.handedness;

import android.os.SystemClock;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class LeftHandedModeLiveSwitchInstrumentedTest {

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
}
