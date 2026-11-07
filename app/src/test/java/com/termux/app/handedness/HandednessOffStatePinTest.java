package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HandednessOffStatePinTest {

    @Test
    public void terminalToolbarBottomContainerKeepsExtraKeysBeforeRightColumn() {
        View root = inflateActivityLayout();
        LinearLayout container = root.findViewById(R.id.terminal_toolbar_bottom_container);

        Assert.assertSame(root.findViewById(R.id.terminal_toolbar_view_pager), container.getChildAt(0));
        Assert.assertSame(root.findViewById(R.id.terminal_toolbar_right_column), container.getChildAt(1));
    }

    @Test
    public void terminalToolbarIconRowKeepsCurrentOrderAndEndGravity() {
        View root = inflateActivityLayout();
        LinearLayout row = root.findViewById(R.id.terminal_toolbar_icon_button_row);

        assertChildren(row, R.id.terminal_toolbar_browser_toggle_button,
            R.id.terminal_toolbar_keyboard_toggle_button,
            R.id.terminal_toolbar_previous_session_button,
            R.id.terminal_toolbar_next_session_button,
            R.id.terminal_toolbar_jump_to_calling_session_button,
            R.id.terminal_toolbar_session_sheet_button,
            R.id.terminal_toolbar_right_drawer_toggle_button);
        Assert.assertEquals(Gravity.END | Gravity.CENTER_VERTICAL, row.getGravity());
    }

    @Test
    public void terminalToolbarTextInputRowKeepsCurrentOrder() {
        View root = inflateActivityLayout();
        LinearLayout row = root.findViewById(R.id.terminal_toolbar_text_input_row);

        assertChildren(row, R.id.terminal_toolbar_text_input_history_button,
            R.id.terminal_toolbar_text_input, R.id.terminal_toolbar_enter_button);
    }

    @Test
    public void apkUpdateIndicatorKeepsBottomEndRuleAndMargins() {
        View root = inflateActivityLayout();
        FloatingActionButton indicator = root.findViewById(R.id.apk_update_floating_indicator);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) indicator.getLayoutParams();

        Assert.assertEquals(RelativeLayout.TRUE,
            params.getRule(RelativeLayout.ALIGN_PARENT_END));
        Assert.assertEquals(0, params.getRule(RelativeLayout.ALIGN_PARENT_START));
        Assert.assertEquals(R.id.terminal_toolbar_bottom_container,
            params.getRule(RelativeLayout.ABOVE));
        Assert.assertEquals(eightDp(root), params.getMarginEnd());
        Assert.assertEquals(0, params.getMarginStart());
        Assert.assertEquals(eightDp(root), params.bottomMargin);
    }

    @Test
    public void sessionInfoBottomContainerKeepsBarsBeforePendingIndicator() {
        View root = inflateActivityLayout();
        LinearLayout row = root.findViewById(R.id.session_info_bottom_container);

        assertChildren(row, R.id.session_info_bars_column, R.id.owner_call_pending_indicator);
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
}
