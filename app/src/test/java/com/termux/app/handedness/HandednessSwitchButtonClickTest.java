package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HandednessSwitchButtonClickTest {

    private static final int WIDTH_PX = 1080;
    private static final int HEIGHT_PX = 1920;

    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        preferences.setLeftHandedModeEnabled(false);
    }

    @Test
    public void tappingTheSwitchButtonFlipsThePreferenceAndMovesTheButtonBetweenOuterEdges() {
        View root = inflateActivityLayout();
        HandednessLayoutApplier applier = new HandednessLayoutApplier();
        applier.registerPart(new TerminalToolbarHandednessPart(root));
        applier.applyHandedness(currentHandedness());
        layoutActivity(root);
        LinearLayout bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View button = root.findViewById(R.id.terminal_toolbar_handedness_switch_button);
        Assert.assertEquals(0, button.getLeft());

        TerminalToolbarHandednessPart.bindSwitchButton(button, preferences);
        button.performClick();

        Assert.assertTrue(preferences.isLeftHandedModeEnabled());
        applier.applyHandedness(currentHandedness());
        layoutActivity(root);
        Assert.assertSame(button, bottom.getChildAt(bottom.getChildCount() - 1));
        Assert.assertEquals(bottom.getWidth(), button.getRight());

        button.performClick();

        Assert.assertFalse(preferences.isLeftHandedModeEnabled());
        applier.applyHandedness(currentHandedness());
        layoutActivity(root);
        Assert.assertSame(button, bottom.getChildAt(0));
        Assert.assertEquals(0, button.getLeft());
    }

    private Handedness currentHandedness() {
        return Handedness.fromLeftHandedModeEnabled(preferences.isLeftHandedModeEnabled());
    }

    private static void layoutActivity(View root) {
        root.findViewById(R.id.apk_update_floating_indicator).setVisibility(View.VISIBLE);
        root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, WIDTH_PX, HEIGHT_PX);
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
