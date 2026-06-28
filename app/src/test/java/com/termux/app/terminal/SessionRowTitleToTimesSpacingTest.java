package com.termux.app.terminal;

import android.content.Context;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionRowTitleToTimesSpacingTest {

    private static final int RENDER_WIDTH_PIXELS = 720;

    @Test
    public void titleBottomPaddingCollapsesToZeroWhenTimesAreShown() {
        Assert.assertEquals(0, TermuxSessionsListViewController.sessionTitleBottomPaddingDp(true));
    }

    @Test
    public void titleKeepsRowBottomPaddingWhenTimesAreHidden() {
        Assert.assertTrue(TermuxSessionsListViewController.sessionTitleBottomPaddingDp(false) > 0);
    }

    @Test
    public void renderedDescriptionSitsTightAgainstTheCallTimesLine() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView title = row.findViewById(R.id.session_title);
        TextView times = row.findViewById(R.id.session_row_times);

        title.setText("claude-main  tighten the description to call spacing");
        times.setText("call: 3h  out: 12m  reply: 45s  sub: 0");
        times.setVisibility(View.VISIBLE);

        applyTitleBottomPadding(context, title, true);
        measure(row);

        int descriptionToCallGapPx = (times.getTop() + times.getPaddingTop())
            - (title.getBottom() - title.getPaddingBottom());
        Assert.assertEquals("the description must sit directly against the call time line "
                + "without an excessive gap", 0, descriptionToCallGapPx);
    }

    @Test
    public void rowKeepsBottomBreathingRoomWhenNoTimesLineIsShown() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView title = row.findViewById(R.id.session_title);
        TextView times = row.findViewById(R.id.session_row_times);

        title.setText("claude-main");
        times.setVisibility(View.GONE);

        applyTitleBottomPadding(context, title, false);
        measure(row);

        Assert.assertTrue("a session row without a times line must keep its bottom padding "
            + "so rows do not touch", title.getPaddingBottom() > 0);
    }

    private static void applyTitleBottomPadding(Context context, TextView title, boolean timesVisible) {
        int bottomPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            TermuxSessionsListViewController.sessionTitleBottomPaddingDp(timesVisible),
            context.getResources().getDisplayMetrics());
        title.setPadding(title.getPaddingLeft(), title.getPaddingTop(),
            title.getPaddingRight(), bottomPx);
    }

    private static void measure(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), Math.max(1, view.getMeasuredHeight()));
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private static View inflateRow(Context context) {
        return LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, null, false);
    }
}
