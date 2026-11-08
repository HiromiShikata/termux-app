package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ToolbarPlacementPinTest {

    private static final int WIDTH_PX = 1080;
    private static final int HEIGHT_PX = 1920;

    @Test
    public void rightHandedExtraKeysAndRightColumnSplitTheToolbarInHalf() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        root.findViewById(R.id.terminal_toolbar_view_pager).setVisibility(View.VISIBLE);
        layoutActivity(root);

        assertSplitInHalf(root);
    }

    @Test
    public void leftHandedExtraKeysAndRightColumnSplitTheToolbarInHalf() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        root.findViewById(R.id.terminal_toolbar_view_pager).setVisibility(View.VISIBLE);
        layoutActivity(root);

        assertSplitInHalf(root);
    }

    @Test
    public void rightColumnTakesTheFullToolbarWidthWhenExtraKeysAreGone() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        layoutActivity(root);

        View bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);
        Assert.assertEquals(bottom.getWidth(), column.getWidth(), 1);
    }

    @Test
    public void leftHandedRightColumnTakesTheFullToolbarWidthWhenExtraKeysAreGone() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        layoutActivity(root);

        View bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);
        Assert.assertEquals(bottom.getWidth(), column.getWidth(), 1);
    }

    private static void assertSplitInHalf(View root) {
        View bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        View pager = root.findViewById(R.id.terminal_toolbar_view_pager);
        View column = root.findViewById(R.id.terminal_toolbar_right_column);
        Assert.assertEquals(bottom.getWidth() / 2, pager.getWidth(), 1);
        Assert.assertEquals(bottom.getWidth() / 2, column.getWidth(), 1);
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
