package com.termux.app.terminal;

import android.content.Context;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionRowCallToUserReasonLineCapTest {

    private static final int MAX_VISIBLE_LINES = 5;

    private TextView inflateSessionTitleView() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);
        return row.findViewById(R.id.session_title);
    }

    @Test
    public void sessionRowReasonTextIsCappedAtFiveLines() {
        TextView sessionTitleView = inflateSessionTitleView();

        Assert.assertEquals(MAX_VISIBLE_LINES, sessionTitleView.getMaxLines());
    }

    @Test
    public void sessionRowReasonTextTruncatesWithATrailingEllipsis() {
        TextView sessionTitleView = inflateSessionTitleView();

        Assert.assertEquals(TextUtils.TruncateAt.END, sessionTitleView.getEllipsize());
    }
}
