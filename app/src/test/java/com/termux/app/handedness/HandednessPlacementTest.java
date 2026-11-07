package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HandednessPlacementTest {

    @Test
    public void leftHandedPlacementMirrorsTheToolbarAndMainScreen() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new MainScreenHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        LinearLayout bottom = root.findViewById(R.id.terminal_toolbar_bottom_container);
        assertChildren(bottom, R.id.terminal_toolbar_right_column, R.id.terminal_toolbar_view_pager);
        LinearLayout iconRow = root.findViewById(R.id.terminal_toolbar_icon_button_row);
        assertChildren(iconRow, R.id.terminal_toolbar_right_drawer_toggle_button,
            R.id.terminal_toolbar_session_sheet_button,
            R.id.terminal_toolbar_jump_to_calling_session_button,
            R.id.terminal_toolbar_next_session_button,
            R.id.terminal_toolbar_previous_session_button,
            R.id.terminal_toolbar_keyboard_toggle_button,
            R.id.terminal_toolbar_browser_toggle_button);
        Assert.assertEquals(Gravity.START | Gravity.CENTER_VERTICAL, iconRow.getGravity());
        LinearLayout textRow = root.findViewById(R.id.terminal_toolbar_text_input_row);
        assertChildren(textRow, R.id.terminal_toolbar_enter_button,
            R.id.terminal_toolbar_text_input, R.id.terminal_toolbar_text_input_history_button);

        RelativeLayout.LayoutParams indicatorParams = indicatorParams(root);
        Assert.assertEquals(RelativeLayout.TRUE,
            indicatorParams.getRule(RelativeLayout.ALIGN_PARENT_START));
        Assert.assertEquals(0, indicatorParams.getRule(RelativeLayout.ALIGN_PARENT_END));
        Assert.assertEquals(R.id.terminal_toolbar_bottom_container,
            indicatorParams.getRule(RelativeLayout.ABOVE));
        Assert.assertEquals(eightDp(root), indicatorParams.getMarginStart());
        Assert.assertEquals(0, indicatorParams.getMarginEnd());
        Assert.assertEquals(eightDp(root), indicatorParams.bottomMargin);
        LinearLayout sessionInfo = root.findViewById(R.id.session_info_bottom_container);
        assertChildren(sessionInfo, R.id.owner_call_pending_indicator, R.id.session_info_bars_column);
    }

    @Test
    public void leftHandedThenRightHandedRestoresTheOriginalPlacement() {
        View root = inflateActivityLayout();
        PlacementSnapshot before = PlacementSnapshot.capture(root);

        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new MainScreenHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        new MainScreenHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);

        before.assertMatches(root);
    }

    @Test
    public void applyingLeftHandedTwiceIsIdempotent() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new MainScreenHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        PlacementSnapshot once = PlacementSnapshot.capture(root);

        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        new MainScreenHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        once.assertMatches(root);
    }

    @Test
    public void handednessLayoutApplierAppliesRegisteredPartsInOrder() {
        HandednessLayoutApplier applier = new HandednessLayoutApplier();
        RecordingPart first = new RecordingPart();
        RecordingPart second = new RecordingPart();
        applier.registerPart(first);
        applier.registerPart(second);

        applier.applyHandedness(Handedness.LEFT_HANDED);

        Assert.assertEquals(Arrays.asList(Handedness.LEFT_HANDED), first.applied);
        Assert.assertEquals(Arrays.asList(Handedness.LEFT_HANDED), second.applied);
    }

    private static RelativeLayout.LayoutParams indicatorParams(View root) {
        return (RelativeLayout.LayoutParams) root.findViewById(
            R.id.apk_update_floating_indicator).getLayoutParams();
    }

    private static void assertChildren(LinearLayout parent, int... ids) {
        Assert.assertEquals(ids.length, parent.getChildCount());
        for (int index = 0; index < ids.length; index++) {
            Assert.assertEquals(ids[index], controlIdOf(parent.getChildAt(index)));
        }
    }

    private static int controlIdOf(View child) {
        if (child.getId() != View.NO_ID) {
            return child.getId();
        }
        return firstImageButton((ViewGroup) child).getId();
    }

    private static ImageButton firstImageButton(ViewGroup group) {
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child instanceof ImageButton) {
                return (ImageButton) child;
            }
            if (child instanceof ViewGroup) {
                ImageButton nested = firstImageButton((ViewGroup) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static int eightDp(View root) {
        return Math.round(8 * root.getResources().getDisplayMetrics().density);
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

    private static final class RecordingPart implements HandednessLayoutPart {
        private final List<Handedness> applied = new ArrayList<>();

        @Override
        public void applyHandedness(Handedness handedness) {
            applied.add(handedness);
        }
    }

    private static final class PlacementSnapshot {
        private final int[] bottomOrder;
        private final int[] iconOrder;
        private final int iconGravity;
        private final int[] textOrder;
        private final int[] indicatorRules;
        private final int indicatorLeftMargin;
        private final int indicatorTopMargin;
        private final int indicatorRightMargin;
        private final int indicatorBottomMargin;
        private final int indicatorStartMargin;
        private final int indicatorEndMargin;
        private final int[] sessionInfoOrder;

        private PlacementSnapshot(View root) {
            bottomOrder = childIds((LinearLayout) root.findViewById(
                R.id.terminal_toolbar_bottom_container));
            LinearLayout iconRow = root.findViewById(R.id.terminal_toolbar_icon_button_row);
            iconOrder = childIds(iconRow);
            iconGravity = iconRow.getGravity();
            textOrder = childIds((LinearLayout) root.findViewById(
                R.id.terminal_toolbar_text_input_row));
            RelativeLayout.LayoutParams params = indicatorParams(root);
            indicatorRules = params.getRules().clone();
            indicatorLeftMargin = params.leftMargin;
            indicatorTopMargin = params.topMargin;
            indicatorRightMargin = params.rightMargin;
            indicatorBottomMargin = params.bottomMargin;
            indicatorStartMargin = params.getMarginStart();
            indicatorEndMargin = params.getMarginEnd();
            sessionInfoOrder = childIds((LinearLayout) root.findViewById(
                R.id.session_info_bottom_container));
        }

        private static PlacementSnapshot capture(View root) {
            return new PlacementSnapshot(root);
        }

        private void assertMatches(View root) {
            Assert.assertArrayEquals(bottomOrder, childIds((LinearLayout) root.findViewById(
                R.id.terminal_toolbar_bottom_container)));
            LinearLayout iconRow = root.findViewById(R.id.terminal_toolbar_icon_button_row);
            Assert.assertArrayEquals(iconOrder, childIds(iconRow));
            Assert.assertEquals(iconGravity, iconRow.getGravity());
            Assert.assertArrayEquals(textOrder, childIds((LinearLayout) root.findViewById(
                R.id.terminal_toolbar_text_input_row)));
            RelativeLayout.LayoutParams params = indicatorParams(root);
            Assert.assertArrayEquals(indicatorRules, params.getRules());
            Assert.assertEquals(indicatorLeftMargin, params.leftMargin);
            Assert.assertEquals(indicatorTopMargin, params.topMargin);
            Assert.assertEquals(indicatorRightMargin, params.rightMargin);
            Assert.assertEquals(indicatorBottomMargin, params.bottomMargin);
            Assert.assertEquals(indicatorStartMargin, params.getMarginStart());
            Assert.assertEquals(indicatorEndMargin, params.getMarginEnd());
            Assert.assertArrayEquals(sessionInfoOrder, childIds((LinearLayout) root.findViewById(
                R.id.session_info_bottom_container)));
        }

        private static int[] childIds(LinearLayout parent) {
            int[] ids = new int[parent.getChildCount()];
            for (int index = 0; index < parent.getChildCount(); index++) {
                ids[index] = controlIdOf(parent.getChildAt(index));
            }
            return ids;
        }
    }
}
