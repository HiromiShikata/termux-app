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
public class TextInputHistoryRowRightHandedPinTest {

    @Test
    public void historyRowKeepsPinFirstAndRestoreLast() {
        View row = inflateHistoryRow();
        new TextInputHistoryRowHandednessLayout().apply(row, Handedness.RIGHT_HANDED);

        LinearLayout layout = (LinearLayout) row;
        assertChildren(layout,
            R.id.toolbar_text_input_history_pin_button,
            R.id.toolbar_text_input_history_entry_scroll,
            R.id.toolbar_text_input_history_edit_button,
            R.id.toolbar_text_input_history_restore_button);
    }

    private static void assertChildren(LinearLayout parent, int... ids) {
        Assert.assertEquals(ids.length, parent.getChildCount());
        for (int index = 0; index < ids.length; index++) {
            Assert.assertSame(parent.findViewById(ids[index]), parent.getChildAt(index));
        }
    }

    private static View inflateHistoryRow() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.item_toolbar_text_input_history, new FrameLayout(context), false);
    }
}
