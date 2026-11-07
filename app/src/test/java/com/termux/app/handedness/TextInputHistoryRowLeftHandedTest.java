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
public class TextInputHistoryRowLeftHandedTest {

    @Test
    public void leftHandedHistoryRowMovesRestoreAndEditLeftAndPinRight() {
        View row = inflateHistoryRow();
        new TextInputHistoryRowHandednessLayout().apply(row, Handedness.LEFT_HANDED);
        assertChildren((LinearLayout) row,
            R.id.toolbar_text_input_history_restore_button,
            R.id.toolbar_text_input_history_edit_button,
            R.id.toolbar_text_input_history_entry_scroll,
            R.id.toolbar_text_input_history_pin_button);
    }

    @Test
    public void leftHandedHistoryRowThenRightHandedRestoresThePinnedState() {
        View row = inflateHistoryRow();
        Snapshot before = Snapshot.capture(row);
        TextInputHistoryRowHandednessLayout layout = new TextInputHistoryRowHandednessLayout();
        layout.apply(row, Handedness.LEFT_HANDED);
        layout.apply(row, Handedness.RIGHT_HANDED);
        before.assertMatches(row);
    }

    @Test
    public void applyingLeftHandedHistoryRowTwiceIsIdempotent() {
        View row = inflateHistoryRow();
        TextInputHistoryRowHandednessLayout layout = new TextInputHistoryRowHandednessLayout();
        layout.apply(row, Handedness.LEFT_HANDED);
        Snapshot once = Snapshot.capture(row);
        layout.apply(row, Handedness.LEFT_HANDED);
        once.assertMatches(row);
    }

    @Test
    public void historyRowInflatedAfterTheSwitchFollowsLeftHandedPlacement() {
        TextInputHistoryRowHandednessLayout layout = new TextInputHistoryRowHandednessLayout();
        layout.apply(inflateHistoryRow(), Handedness.LEFT_HANDED);
        View laterRow = inflateHistoryRow();
        layout.apply(laterRow, Handedness.LEFT_HANDED);
        assertChildren((LinearLayout) laterRow,
            R.id.toolbar_text_input_history_restore_button,
            R.id.toolbar_text_input_history_edit_button,
            R.id.toolbar_text_input_history_entry_scroll,
            R.id.toolbar_text_input_history_pin_button);
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
        return LayoutInflater.from(context).inflate(R.layout.item_toolbar_text_input_history,
            new FrameLayout(context), false);
    }

    private static final class Snapshot {
        private final View[] children;

        private Snapshot(View row) {
            LinearLayout layout = (LinearLayout) row;
            children = new View[layout.getChildCount()];
            for (int index = 0; index < children.length; index++) {
                children[index] = layout.getChildAt(index);
            }
        }

        private static Snapshot capture(View row) {
            return new Snapshot(row);
        }

        private void assertMatches(View row) {
            LinearLayout layout = (LinearLayout) row;
            for (int index = 0; index < children.length; index++) {
                Assert.assertSame(children[index], layout.getChildAt(index));
            }
        }
    }
}
