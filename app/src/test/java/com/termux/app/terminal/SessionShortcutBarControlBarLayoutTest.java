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

    @Test
    public void controlIconButtonsAreRightAlignedInTheirRow() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ViewGroup controlsGroup = controlBar.findViewById(R.id.session_list_bottom_sheet_controls_group);
        ShortcutFlowLayout shortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_shortcuts_container);
        addShortcutButtons(context, shortcutsContainer);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        View firstControlButton = firstVisibleChild(controlsGroup);
        View lastControlButton = lastVisibleChild(controlsGroup);
        int groupRightContentEdge = controlsGroup.getWidth() - controlsGroup.getPaddingRight();
        assertTrue("the rightmost control button must reach the right edge of the controls row",
            lastControlButton.getRight() >= groupRightContentEdge - 1);
        assertTrue("the control buttons must be pushed to the right, leaving free space on the left",
            firstControlButton.getLeft() > controlsGroup.getPaddingLeft());
    }

    @Test
    public void everyShortcutRowIsRightJustifiedAgainstTheVisibleRightEdge() {
        Context context = themedContext();
        ViewGroup controlBar = inflateControlBar(context);
        ShortcutFlowLayout shortcutsContainer =
            controlBar.findViewById(R.id.session_list_bottom_sheet_shortcuts_container);
        addShortcutButtons(context, shortcutsContainer);

        measureAndLayout(controlBar, BAR_WIDTH_PIXELS);

        int visibleRightEdge = controlBar.getWidth() - controlBar.getPaddingRight();
        int rightMargin = rightMarginOf(shortcutsContainer.getChildAt(0));
        int previousTop = Integer.MIN_VALUE;
        int rowRightEdgeInBar = 0;
        for (int index = 0; index < shortcutsContainer.getChildCount(); index++) {
            View shortcut = shortcutsContainer.getChildAt(index);
            int shortcutRightEdgeInBar = shortcutsContainer.getLeft() + shortcut.getRight();
            if (shortcut.getTop() != previousTop) {
                if (index != 0) {
                    assertRowRightJustified(rowRightEdgeInBar, visibleRightEdge, rightMargin);
                }
                previousTop = shortcut.getTop();
                rowRightEdgeInBar = shortcutRightEdgeInBar;
            } else {
                rowRightEdgeInBar = Math.max(rowRightEdgeInBar, shortcutRightEdgeInBar);
            }
        }
        assertRowRightJustified(rowRightEdgeInBar, visibleRightEdge, rightMargin);

        View rightmostShortcut =
            shortcutsContainer.getChildAt(shortcutsContainer.getChildCount() - 1);
        int rightmostRightEdgeInBar = shortcutsContainer.getLeft() + rightmostShortcut.getRight();
        assertRowRightJustified(rightmostRightEdgeInBar, visibleRightEdge, rightMargin);
    }

    private static void assertRowRightJustified(int rowRightEdge, int visibleRightEdge, int rightMargin) {
        assertTrue("a shortcut row must not extend past the visible right edge",
            rowRightEdge <= visibleRightEdge);
        assertTrue("each shortcut row must be right-justified so its rightmost button ends at the visible right edge",
            rowRightEdge >= visibleRightEdge - rightMargin - 1);
    }

    private static int rightMarginOf(View child) {
        return ((ViewGroup.MarginLayoutParams) child.getLayoutParams()).rightMargin;
    }

    private static View firstVisibleChild(ViewGroup parent) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (child.getVisibility() != View.GONE) {
                return child;
            }
        }
        throw new AssertionError("expected at least one visible child");
    }

    private static View lastVisibleChild(ViewGroup parent) {
        for (int index = parent.getChildCount() - 1; index >= 0; index--) {
            View child = parent.getChildAt(index);
            if (child.getVisibility() != View.GONE) {
                return child;
            }
        }
        throw new AssertionError("expected at least one visible child");
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
