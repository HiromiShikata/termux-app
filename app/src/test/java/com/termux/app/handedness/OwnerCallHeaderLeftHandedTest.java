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
public class OwnerCallHeaderLeftHandedTest {

    @Test
    public void leftHandedOwnerCallHeaderPutsCloseFirstAndSwapsPositionMargins() {
        View root = inflateActivityLayout();
        new OwnerCallHeaderHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
        assertChildren(header, R.id.owner_call_dialog_close_button,
            R.id.owner_call_dialog_previous_button, R.id.owner_call_dialog_next_button,
            R.id.owner_call_dialog_position, R.id.owner_call_dialog_relative_time);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
            root.findViewById(R.id.owner_call_dialog_position).getLayoutParams();
        Assert.assertEquals(dp(root, 2), params.getMarginStart());
        Assert.assertEquals(dp(root, 0), params.getMarginEnd());
    }

    @Test
    public void leftHandedOwnerCallHeaderThenRightHandedRestoresThePinnedState() {
        View root = inflateActivityLayout();
        Snapshot before = Snapshot.capture(root);
        OwnerCallHeaderHandednessPart part = new OwnerCallHeaderHandednessPart(root);
        part.applyHandedness(Handedness.LEFT_HANDED);
        part.applyHandedness(Handedness.RIGHT_HANDED);
        before.assertMatches(root);
    }

    @Test
    public void applyingLeftHandedOwnerCallHeaderTwiceIsIdempotent() {
        View root = inflateActivityLayout();
        OwnerCallHeaderHandednessPart part = new OwnerCallHeaderHandednessPart(root);
        part.applyHandedness(Handedness.LEFT_HANDED);
        Snapshot once = Snapshot.capture(root);
        part.applyHandedness(Handedness.LEFT_HANDED);
        once.assertMatches(root);
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
        return LayoutInflater.from(context).inflate(R.layout.activity_termux,
            new FrameLayout(context), false);
    }

    private static final class Snapshot {
        private final View[] children;
        private final int marginStart;
        private final int marginEnd;

        private Snapshot(View root) {
            LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
            children = new View[header.getChildCount()];
            for (int index = 0; index < children.length; index++) {
                children[index] = header.getChildAt(index);
            }
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                root.findViewById(R.id.owner_call_dialog_position).getLayoutParams();
            marginStart = params.getMarginStart();
            marginEnd = params.getMarginEnd();
        }

        private static Snapshot capture(View root) {
            return new Snapshot(root);
        }

        private void assertMatches(View root) {
            LinearLayout header = root.findViewById(R.id.owner_call_dialog_header);
            for (int index = 0; index < children.length; index++) {
                Assert.assertSame(children[index], header.getChildAt(index));
            }
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                root.findViewById(R.id.owner_call_dialog_position).getLayoutParams();
            Assert.assertEquals(marginStart, params.getMarginStart());
            Assert.assertEquals(marginEnd, params.getMarginEnd());
        }
    }
}
