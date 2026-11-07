package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class IconRowScrollEdgeTest {

    private static final int VIEWPORT_WIDTH_PX = 200;
    private static final int ROOT_WIDTH_PX = 1080;
    private static final int ROOT_HEIGHT_PX = 1920;

    @Test
    public void rightHandedShowsTheBrowserAndKeyboardTogglesAtTheStartOfTheIconRow() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);
        layout(root);

        assertIconRowViewport(root, 0, true);
    }

    @Test
    public void leftHandedShowsTheSameBrowserAndKeyboardTogglesAtTheEndOfTheReversedIconRow() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        layout(root);

        assertIconRowViewport(root, -1, false);
    }

    @Test
    public void leftHandedScrollEdgeSurvivesARelayoutAtTheSameWidth() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        layout(root);
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        int firstScrollX = scroll.getScrollX();

        layout(root);

        Assert.assertEquals("a relayout must keep the left-handed end position", firstScrollX,
            scroll.getScrollX());
    }

    @Test
    public void rightHandedManualScrollSurvivesARelayoutAndReapplyingRightHanded() {
        View root = inflateActivityLayout();
        TerminalToolbarHandednessPart part = new TerminalToolbarHandednessPart(root);
        part.applyHandedness(Handedness.RIGHT_HANDED);
        layout(root);
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        scroll.scrollTo(40, 0);

        root.requestLayout();
        layout(root);
        Assert.assertEquals("a relayout must keep a manual right-handed scroll", 40, scroll.getScrollX());

        part.applyHandedness(Handedness.RIGHT_HANDED);
        layout(root);
        Assert.assertEquals("re-applying right-handed must not reset a manual scroll", 40, scroll.getScrollX());
    }

    @Test
    public void leftHandedManualScrollSurvivesARelayoutWithoutASizeChange() {
        View root = inflateActivityLayout();
        new TerminalToolbarHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        layout(root);
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        scroll.scrollTo(10, 0);

        layout(root);
        Assert.assertEquals("a same-size relayout must keep a manual left-handed scroll", 10, scroll.getScrollX());
    }

    @Test
    public void switchingLeftToRightRestoresTheStartAndRightToLeftShowsTheEnd() {
        View root = inflateActivityLayout();
        TerminalToolbarHandednessPart part = new TerminalToolbarHandednessPart(root);
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        LinearLayout row = root.findViewById(R.id.terminal_toolbar_icon_button_row);
        part.applyHandedness(Handedness.LEFT_HANDED);
        layout(root);
        int maxScrollX = row.getWidth() - scroll.getWidth();
        Assert.assertEquals("left-handed shows the end", maxScrollX, scroll.getScrollX());

        part.applyHandedness(Handedness.RIGHT_HANDED);
        Assert.assertEquals("switching to right-handed restores the start at once", 0, scroll.getScrollX());
        layout(root);
        Assert.assertEquals("and keeps the start after the next layout", 0, scroll.getScrollX());

        part.applyHandedness(Handedness.LEFT_HANDED);
        layout(root);
        Assert.assertEquals("switching back to left-handed shows the end", maxScrollX, scroll.getScrollX());
    }

    private static void assertIconRowViewport(View root, int expectedScrollSign, boolean rightHanded) {
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        LinearLayout row = root.findViewById(R.id.terminal_toolbar_icon_button_row);
        Assert.assertTrue("the icon row must overflow the viewport for this test to mean anything",
            row.getWidth() > scroll.getWidth());
        int maxScrollX = row.getWidth() - scroll.getWidth();
        String mode = rightHanded ? "right-handed" : "left-handed";
        if (expectedScrollSign < 0) {
            Assert.assertEquals(mode + " must scroll to the end of the row", maxScrollX, scroll.getScrollX());
        } else {
            Assert.assertEquals(mode + " must scroll to the start of the row", 0, scroll.getScrollX());
        }

        assertFullyVisible(scroll, row, R.id.terminal_toolbar_browser_toggle_button, true, mode);
        assertFullyVisible(scroll, row, R.id.terminal_toolbar_keyboard_toggle_button, true, mode);
        assertFullyVisible(scroll, row, R.id.terminal_toolbar_session_sheet_button, false, mode);
        assertFullyVisible(scroll, row, R.id.terminal_toolbar_right_drawer_toggle_button, false, mode);
    }

    private static void assertFullyVisible(HorizontalScrollView scroll, LinearLayout row, int buttonId,
                                           boolean expectedVisible, String mode) {
        View slot = slotOf(row, row.findViewById(buttonId));
        int left = row.getLeft() + slot.getLeft();
        int right = row.getLeft() + slot.getRight();
        boolean visible = left >= scroll.getScrollX() && right <= scroll.getScrollX() + scroll.getWidth();
        Assert.assertEquals(mode + " button " + buttonId + " fully inside the viewport", expectedVisible, visible);
    }

    private static View slotOf(LinearLayout row, View button) {
        View slot = button;
        while (slot.getParent() != row) {
            slot = (View) slot.getParent();
        }
        return slot;
    }

    private static void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(ROOT_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(ROOT_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, ROOT_WIDTH_PX, ROOT_HEIGHT_PX);
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        FrameLayout parent = new FrameLayout(context);
        View root = LayoutInflater.from(context).inflate(R.layout.activity_termux, parent, false);
        HorizontalScrollView scroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        ViewGroup.LayoutParams params = scroll.getLayoutParams();
        params.width = VIEWPORT_WIDTH_PX;
        scroll.setLayoutParams(params);
        return root;
    }
}
