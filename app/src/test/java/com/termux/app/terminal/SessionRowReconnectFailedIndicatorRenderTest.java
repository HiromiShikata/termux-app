package com.termux.app.terminal;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.GraphicsMode;

/**
 * Proves the bottom-sheet session-row reconnect-failed "tap to retry" indicator occupies the same
 * fixed reserved slot as the spinner, so a row toggling between reconnecting, failed and idle keeps a
 * stable height, and the failed indicator never collapses its slot.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionRowReconnectFailedIndicatorRenderTest {

    private static View buildMeasuredRow(Context context, int spinnerVisibility,
                                         int failedVisibility) {
        View row = View.inflate(context, R.layout.item_terminal_sessions_list, null);
        row.setBackgroundColor(0xFFFFFFFF);
        TextView title = row.findViewById(R.id.session_title);
        title.setText("agent-session");
        title.setTextColor(0xFF202020);
        row.findViewById(R.id.session_reconnecting_indicator).setVisibility(spinnerVisibility);
        row.findViewById(R.id.session_reconnect_failed_indicator).setVisibility(failedVisibility);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        row.measure(widthSpec, heightSpec);
        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
        return row;
    }

    @Test
    public void togglingBetweenSpinnerFailedAndIdleDoesNotChangeTheRowHeight() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View spinningRow = buildMeasuredRow(context, View.VISIBLE, View.INVISIBLE);
        View failedRow = buildMeasuredRow(context, View.INVISIBLE, View.VISIBLE);
        View idleRow = buildMeasuredRow(context, View.INVISIBLE, View.INVISIBLE);

        Assert.assertEquals("the failed indicator must not change the row height",
            idleRow.getMeasuredHeight(), failedRow.getMeasuredHeight());
        Assert.assertEquals("spinner and failed indicator share one fixed slot",
            spinningRow.getMeasuredHeight(), failedRow.getMeasuredHeight());
        controller.destroy();
    }

    @Test
    public void theFailedIndicatorReservesItsSlotWithInvisibleNeverGone() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View idleRow = buildMeasuredRow(context, View.INVISIBLE, View.INVISIBLE);
        View failedIndicator = idleRow.findViewById(R.id.session_reconnect_failed_indicator);

        Assert.assertNotEquals("the failed indicator must reserve its slot with INVISIBLE, never GONE",
            View.GONE, failedIndicator.getVisibility());
        Assert.assertTrue("the reserved failed-indicator slot must have a fixed non-zero size",
            failedIndicator.getMeasuredWidth() > 0 && failedIndicator.getMeasuredHeight() > 0);
        controller.destroy();
    }
}
