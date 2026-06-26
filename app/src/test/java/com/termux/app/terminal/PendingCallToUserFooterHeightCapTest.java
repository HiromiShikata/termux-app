package com.termux.app.terminal;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PendingCallToUserFooterHeightCapTest {

    private static final int WIDTH_PIXELS = 300;

    private View inflateActivityTermux() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

    private MaxHeightScrollView callToUserScroll() {
        View root = inflateActivityTermux();
        View scroll = root.findViewById(R.id.session_pending_call_to_user_scroll);
        Assert.assertTrue(
            "call-to-user container must be a MaxHeightScrollView so its height is genuinely capped",
            scroll instanceof MaxHeightScrollView);
        return (MaxHeightScrollView) scroll;
    }

    @Test
    public void callToUserScrollContainerHasAnEffectivePositiveMaxHeight() {
        MaxHeightScrollView scroll = callToUserScroll();

        Assert.assertTrue(scroll.getMaxScrollHeightPixels() > 0);
    }

    @Test
    public void manyLongEntriesKeepTheFooterCappedAndInternallyScrollable() {
        MaxHeightScrollView scroll = callToUserScroll();
        TextView text = scroll.findViewById(R.id.session_pending_call_to_user_text);

        StringBuilder hugeReport = new StringBuilder();
        for (int entry = 0; entry < 200; entry++) {
            hugeReport.append("call-to-user reason number ").append(entry)
                .append(" with a long descriptive sentence that wraps across the footer\n");
        }
        text.setText(hugeReport.toString());

        scroll.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        scroll.layout(0, 0, scroll.getMeasuredWidth(), scroll.getMeasuredHeight());

        Assert.assertEquals(scroll.getMaxScrollHeightPixels(), scroll.getMeasuredHeight());

        int childHeight = scroll.getChildAt(0).getMeasuredHeight();
        Assert.assertTrue("the report content must exceed the capped footer height",
            childHeight > scroll.getMeasuredHeight());
    }

    @Test
    public void textViewIsNotLineCappedSoTheFullReportRemainsScrollable() {
        MaxHeightScrollView scroll = callToUserScroll();
        TextView text = scroll.findViewById(R.id.session_pending_call_to_user_text);

        Assert.assertEquals(Integer.MAX_VALUE, text.getMaxLines());
        Assert.assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, text.getLayoutParams().height);
    }
}
