package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallHeaderTimeGapTest {

    private static final int ROOT_WIDTH_PX = 1080;
    private static final int ROOT_HEIGHT_PX = 1920;
    private static final int HEADER_WIDTH_PX = 600;

    @Test
    public void leftHandedRelativeTimeKeepsTheSameGapFromTheCountAsRightHandedAndRightHandedIsUnchanged() {
        View root = inflateLayoutWithAHeaderOfAFixedWidth();
        TextView time = root.findViewById(R.id.owner_call_dialog_relative_time);
        TextView count = root.findViewById(R.id.owner_call_dialog_position);
        time.setText("2 min ago");
        count.setText("1 / 3");
        int originalTimeGravity = time.getGravity();
        layout(root);
        int rightHandedGap = gapBetweenCountAndTimeText(time, count, Handedness.RIGHT_HANDED);
        Assert.assertEquals("right-handed time gravity must stay as the layout defines it",
            originalTimeGravity, time.getGravity());

        new OwnerCallHeaderHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);
        layout(root);
        int leftHandedGap = gapBetweenCountAndTimeText(time, count, Handedness.LEFT_HANDED);

        Assert.assertTrue("left-handed count must sit left of the time text", leftHandedGap > 0);
        Assert.assertEquals("left-handed gap between the count and the time text must equal the right-handed gap",
            rightHandedGap, leftHandedGap);
    }

    private static int gapBetweenCountAndTimeText(TextView time, TextView count, Handedness handedness) {
        int textWidth = (int) time.getLayout().getLineWidth(0);
        if (handedness == Handedness.LEFT_HANDED) {
            int textLeft = time.getRight() - time.getPaddingRight() - textWidth;
            return textLeft - count.getRight();
        }
        int textLeft = time.getLeft() + time.getPaddingLeft();
        return count.getLeft() - (textLeft + textWidth);
    }

    private static void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(ROOT_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(ROOT_HEIGHT_PX, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, ROOT_WIDTH_PX, ROOT_HEIGHT_PX);
    }

    private static View inflateLayoutWithAHeaderOfAFixedWidth() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        View root = LayoutInflater.from(context).inflate(R.layout.activity_termux, new FrameLayout(context), false);
        root.findViewById(R.id.owner_call_dialog).setVisibility(View.VISIBLE);
        LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
        header.getLayoutParams().width = HEADER_WIDTH_PX;
        header.setLayoutParams(header.getLayoutParams());
        return root;
    }
}
