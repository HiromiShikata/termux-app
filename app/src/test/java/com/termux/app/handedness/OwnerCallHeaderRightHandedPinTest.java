package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallHeaderRightHandedPinTest {

    @Test
    public void ownerCallHeaderKeepsRelativeTimeFirstAndCloseLast() {
        View root = inflateActivityLayout();
        new OwnerCallHeaderHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);

        LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
        assertChildren(header,
            R.id.owner_call_dialog_relative_time,
            R.id.owner_call_dialog_position,
            R.id.owner_call_dialog_previous_button,
            R.id.owner_call_dialog_next_button,
            R.id.owner_call_dialog_close_button);

        View position = root.findViewById(R.id.owner_call_dialog_position);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) position.getLayoutParams();
        Assert.assertEquals(dp(root, 0), layoutParams.getMarginStart());
        Assert.assertEquals(dp(root, 2), layoutParams.getMarginEnd());
    }

    private static void assertChildren(LinearLayout parent, int... ids) {
        Assert.assertEquals(ids.length, parent.getChildCount());
        for (int index = 0; index < ids.length; index++) {
            Assert.assertSame(parent.findViewById(ids[index]), parent.getChildAt(index));
        }
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
