package com.termux.app.terminal;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TerminalToolbarIconButtonRowReachabilityTest {

    private static final int NARROW_PHONE_WIDTH_DP = 360;
    private static final int TOOLBAR_HEIGHT_DP = 32;

    @Test
    public void drawerToggleButtonExistsInTheBottomToolbar() {
        View bottomContainer = inflateBottomToolbar();

        Assert.assertNotNull(bottomContainer.findViewById(R.id.terminal_toolbar_right_drawer_toggle_button));
    }

    @Test
    public void allIconButtonsLiveInsideAHorizontallyScrollableContainerSoNoneAreClipped() {
        View bottomContainer = inflateBottomToolbar();

        View drawerToggleButton = bottomContainer.findViewById(R.id.terminal_toolbar_right_drawer_toggle_button);

        Assert.assertTrue(isDescendantOfHorizontalScrollView(drawerToggleButton));
    }

    @Test
    public void everyIconButtonKeepsFullWidthAndStaysReachableWhenExtraKeysToolbarHalvesTheWidth() {
        Context context = themedContext();
        View bottomContainer = inflateBottomToolbar(context);
        bottomContainer.findViewById(R.id.terminal_toolbar_view_pager).setVisibility(View.VISIBLE);

        int widthPx = dpToPx(context, NARROW_PHONE_WIDTH_DP);
        int heightPx = dpToPx(context, TOOLBAR_HEIGHT_DP);
        bottomContainer.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
        bottomContainer.layout(0, 0, bottomContainer.getMeasuredWidth(), bottomContainer.getMeasuredHeight());

        HorizontalScrollView scrollView = bottomContainer.findViewById(R.id.terminal_toolbar_icon_button_scroll);
        ViewGroup iconButtonRow = bottomContainer.findViewById(R.id.terminal_toolbar_icon_button_row);

        int scrollableContentWidth = iconButtonRow.getWidth();
        int maxScrollX = Math.max(0, scrollableContentWidth - scrollView.getWidth());

        for (int index = 0; index < iconButtonRow.getChildCount(); index++) {
            Assert.assertTrue(iconButtonRow.getChildAt(index).getWidth() > 0);
        }

        View drawerToggleButton = bottomContainer.findViewById(R.id.terminal_toolbar_right_drawer_toggle_button);

        Assert.assertTrue(buttonRightEdgeReachable(drawerToggleButton, scrollView, maxScrollX));
    }

    private static boolean buttonRightEdgeReachable(View button, HorizontalScrollView scrollView, int maxScrollX) {
        int buttonRightWithinRow = button.getRight();
        int visibleRightWhenScrolledToEnd = maxScrollX + scrollView.getWidth();
        return buttonRightWithinRow <= visibleRightWhenScrolledToEnd;
    }

    private static boolean isDescendantOfHorizontalScrollView(View view) {
        View current = view;
        while (current != null) {
            if (current instanceof HorizontalScrollView) {
                return true;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                return false;
            }
        }
        return false;
    }

    private static View inflateBottomToolbar() {
        return inflateBottomToolbar(themedContext());
    }

    private static View inflateBottomToolbar(Context context) {
        View root = LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
        return root.findViewById(R.id.terminal_toolbar_bottom_container);
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }
}
