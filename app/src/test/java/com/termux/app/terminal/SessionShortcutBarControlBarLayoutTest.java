package com.termux.app.terminal;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionShortcutBarControlBarLayoutTest {

    private static final int BAR_WIDTH_PIXELS = 720;

    @Test
    public void shortcutRowSpansFullBarWidthOnItsOwnLineBelowTheControls() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        View controlsGroup = controlBar.findViewById(R.id.session_list_bottom_sheet_controls_group);
        ShortcutFlowLayout shortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_shortcuts_container);
        addShortcutButtons(context, shortcutsContainer);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        int barContentWidth = controlBar.getWidth() - controlBar.getPaddingLeft() - controlBar.getPaddingRight();
        assertTrue("the shortcut row must sit on its own line below the control buttons",
            shortcutsContainer.getTop() >= controlsGroup.getBottom());
        assertTrue("the shortcut row must span the full bar width, not share a horizontal row",
            shortcutsContainer.getWidth() >= barContentWidth);
    }

    @Test
    public void everyShortcutIncludingTheRightmostAlwaysNaStaysWithinTheVisibleBarWidth() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ShortcutFlowLayout shortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_shortcuts_container);
        addShortcutButtons(context, shortcutsContainer);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        int visibleRightEdge = controlBar.getWidth() - controlBar.getPaddingRight();
        for (int index = 0; index < shortcutsContainer.getChildCount(); index++) {
            View shortcut = shortcutsContainer.getChildAt(index);
            int shortcutRightEdgeInBar = shortcutsContainer.getLeft() + shortcut.getRight();
            int shortcutLeftEdgeInBar = shortcutsContainer.getLeft() + shortcut.getLeft();
            assertTrue("shortcut left edge must stay within the visible bar",
                shortcutLeftEdgeInBar >= controlBar.getPaddingLeft());
            assertTrue("shortcut right edge must stay within the visible bar width so it is not clipped",
                shortcutRightEdgeInBar <= visibleRightEdge);
        }
        View lastShortcut = shortcutsContainer.getChildAt(shortcutsContainer.getChildCount() - 1);
        int lastShortcutRightEdgeInBar = shortcutsContainer.getLeft() + lastShortcut.getRight();
        assertTrue("the rightmost always-NA shortcut must be fully within the visible bar width",
            lastShortcutRightEdgeInBar <= visibleRightEdge);
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private static ViewGroup inflateControlBar(Context context) {
        return (ViewGroup) LayoutInflater.from(context)
            .inflate(R.layout.session_list_bottom_sheet_control_bar, null);
    }

    private static void addShortcutButtons(Context context, ShortcutFlowLayout shortcutsContainer) {
        for (int index = 0; index < 12; index++) {
            shortcutsContainer.addView(
                SessionListBottomSheetController.newShortcutButtonView(context, "project" + index + "pm"));
        }
        shortcutsContainer.addView(
            SessionListBottomSheetController.newShortcutButtonView(context, "secretary"));
    }

    private static void measureAndLayout(ViewGroup controlBar, int exactWidthPixels) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(exactWidthPixels, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        controlBar.measure(widthSpec, heightSpec);
        controlBar.layout(0, 0, controlBar.getMeasuredWidth(), controlBar.getMeasuredHeight());
    }
}
