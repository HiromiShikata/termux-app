package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionRowRightHandedPinTest {

    @Test
    public void sessionRowKeepsTextBeforeReconnectAndToggleWithOriginalMargins() {
        View row = inflateSessionRow();
        new SessionRowHandednessLayout().apply(row, Handedness.RIGHT_HANDED);

        View title = row.findViewById(R.id.session_title);
        View contentRow = (View) title.getParent().getParent();
        View reconnect = (View) row.findViewById(R.id.session_reconnecting_indicator).getParent();
        View toggle = row.findViewById(R.id.session_disable_toggle);

        Assert.assertSame(row.findViewById(R.id.session_active_indicator_bar),
            ((ViewGroup) contentRow).getChildAt(0));
        Assert.assertSame(title.getParent(), ((ViewGroup) contentRow).getChildAt(1));
        Assert.assertSame(reconnect, ((ViewGroup) contentRow).getChildAt(2));
        Assert.assertSame(toggle, ((ViewGroup) contentRow).getChildAt(3));

        ViewGroup.MarginLayoutParams reconnectParams =
            (ViewGroup.MarginLayoutParams) reconnect.getLayoutParams();
        Assert.assertEquals(dp(row, 2), reconnectParams.getMarginStart());
        Assert.assertEquals(dp(row, 0), reconnectParams.getMarginEnd());
        ViewGroup.MarginLayoutParams toggleParams =
            (ViewGroup.MarginLayoutParams) toggle.getLayoutParams();
        Assert.assertEquals(dp(row, 16), toggleParams.getMarginStart());
        Assert.assertEquals(dp(row, 6), toggleParams.getMarginEnd());
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static View inflateSessionRow() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);
    }
}
