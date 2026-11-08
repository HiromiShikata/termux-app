package com.termux.app.handedness;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HandednessSwitchButtonPlacementTest {

    private static final int WIDTH_PX = 1080;
    private static final int HEIGHT_PX = 1920;
    private static final int SWITCH_BUTTON_WIDTH_DP = 36;

    @Test
    public void rightHandedSwitchButtonIsTheLeftmostToolbarChildBesideExtraKeysAndColumn() {
        View root = inflateActivityLayout();
        applyWithExtraKeysVisible(root, Handedness.RIGHT_HANDED);
        LinearLayout bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View button = root.findViewById(R.id.terminal_toolbar_handedness_switch_button);
        View pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertSame(button, bottom.getChildAt(0));
        Assert.assertSame(pager, bottom.getChildAt(1));
        Assert.assertSame(column, bottom.getChildAt(2));
        Assert.assertEquals(0, button.getLeft());
        Assert.assertEquals(switchButtonWidthPx(root), button.getWidth());
        Assert.assertEquals(column.getHeight(), button.getHeight());
        Assert.assertEquals(button.getRight(), pager.getLeft());
        Assert.assertEquals(pager.getRight(), column.getLeft());
    }

    @Test
    public void leftHandedSwitchButtonIsTheRightmostToolbarChildBesideExtraKeysAndColumn() {
        View root = inflateActivityLayout();
        applyWithExtraKeysVisible(root, Handedness.LEFT_HANDED);
        LinearLayout bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View button = root.findViewById(R.id.terminal_toolbar_handedness_switch_button);
        View pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertSame(column, bottom.getChildAt(0));
        Assert.assertSame(pager, bottom.getChildAt(1));
        Assert.assertSame(button, bottom.getChildAt(2));
        Assert.assertEquals(bottom.getWidth(), button.getRight());
        Assert.assertEquals(switchButtonWidthPx(root), button.getWidth());
        Assert.assertEquals(column.getHeight(), button.getHeight());
        Assert.assertEquals(button.getLeft(), pager.getRight());
        Assert.assertEquals(pager.getLeft(), column.getRight());
    }

    @Test
    public void rightHandedRightColumnKeepsHalfTheWidthAndExtraKeysLoseTheButtonWidth() {
        View root = inflateActivityLayout();
        applyWithExtraKeysVisible(root, Handedness.RIGHT_HANDED);
        int containerWidth = containerWidth(root);
        View pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertEquals(containerWidth / 2, column.getWidth(), 1);
        Assert.assertEquals(containerWidth / 2 - switchButtonWidthPx(root), pager.getWidth(), 1);
    }

    @Test
    public void leftHandedRightColumnKeepsHalfTheWidthAndExtraKeysLoseTheButtonWidth() {
        View root = inflateActivityLayout();
        applyWithExtraKeysVisible(root, Handedness.LEFT_HANDED);
        int containerWidth = containerWidth(root);
        View pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertEquals(containerWidth / 2, column.getWidth(), 1);
        Assert.assertEquals(containerWidth / 2 - switchButtonWidthPx(root), pager.getWidth(), 1);
    }

    @Test
    public void rightHandedRightColumnTakesContainerWidthMinusButtonWhenExtraKeysAreGone() {
        View root = inflateActivityLayout();
        applyWithExtraKeysGone(root, Handedness.RIGHT_HANDED);
        int containerWidth = containerWidth(root);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertEquals(containerWidth - switchButtonWidthPx(root), column.getWidth(), 1);
    }

    @Test
    public void leftHandedRightColumnTakesContainerWidthMinusButtonWhenExtraKeysAreGone() {
        View root = inflateActivityLayout();
        applyWithExtraKeysGone(root, Handedness.LEFT_HANDED);
        int containerWidth = containerWidth(root);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);

        Assert.assertEquals(containerWidth - switchButtonWidthPx(root), column.getWidth(), 1);
    }

    @Test
    public void switchButtonIsNotShownWhenTheWholeToolbarIsHidden() {
        View root = inflateActivityLayout();
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        activity.setContentView(root);
        View bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View button = root.findViewById(R.id.terminal_toolbar_handedness_switch_button);
        Assert.assertTrue(button.isShown());

        bottom.setVisibility(View.GONE);

        Assert.assertEquals(View.VISIBLE, button.getVisibility());
        Assert.assertSame(bottom, button.getParent());
        Assert.assertFalse(button.isShown());
    }

    private static void applyWithExtraKeysVisible(View root, Handedness handedness) {
        new TerminalToolbarHandednessPart(root).applyHandedness(handedness);
        root.findViewById(R.id.terminal_toolbar_view_pager).setVisibility(View.VISIBLE);
        layoutActivity(root);
    }

    private static void applyWithExtraKeysGone(View root, Handedness handedness) {
        new TerminalToolbarHandednessPart(root).applyHandedness(handedness);
        root.findViewById(R.id.terminal_toolbar_view_pager).setVisibility(View.GONE);
        layoutActivity(root);
    }

    private static void layoutActivity(View root) {
        root.findViewById(R.id.apk_update_floating_indicator).setVisibility(View.VISIBLE);
        root.measure(View.MeasureSpec.makeMeasureSpec(WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, WIDTH_PX, HEIGHT_PX);
    }

    private static int containerWidth(View root) {
        return root.findViewById(R.id.terminal_toolbar_bottom_container).getWidth();
    }

    private static int switchButtonWidthPx(View root) {
        return Math.round(SWITCH_BUTTON_WIDTH_DP * root.getResources().getDisplayMetrics().density);
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
