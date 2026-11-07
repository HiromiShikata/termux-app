package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionRowLeftHandedTest {

    @Test
    public void leftHandedSessionRowMovesReconnectAndToggleToTheLeft() {
        View row = inflateSessionRow();
        new SessionRowHandednessLayout().apply(row, Handedness.LEFT_HANDED);
        assertLeft(row);
    }

    @Test
    public void leftHandedSessionRowThenRightHandedRestoresThePinnedState() {
        View row = inflateSessionRow();
        Snapshot before = Snapshot.capture(row);
        SessionRowHandednessLayout layout = new SessionRowHandednessLayout();
        layout.apply(row, Handedness.LEFT_HANDED);
        layout.apply(row, Handedness.RIGHT_HANDED);
        before.assertMatches(row);
    }

    @Test
    public void applyingLeftHandedSessionRowTwiceIsIdempotent() {
        View row = inflateSessionRow();
        SessionRowHandednessLayout layout = new SessionRowHandednessLayout();
        layout.apply(row, Handedness.LEFT_HANDED);
        Snapshot once = Snapshot.capture(row);
        layout.apply(row, Handedness.LEFT_HANDED);
        once.assertMatches(row);
    }

    @Test
    public void sessionRowInflatedAfterTheSwitchFollowsLeftHandedPlacement() {
        SessionRowHandednessLayout layout = new SessionRowHandednessLayout();
        layout.apply(inflateSessionRow(), Handedness.LEFT_HANDED);
        View laterRow = inflateSessionRow();
        layout.apply(laterRow, Handedness.LEFT_HANDED);
        assertLeft(laterRow);
    }

    private static void assertLeft(View row) {
        ViewGroup content = contentRow(row);
        Assert.assertSame(row.findViewById(R.id.session_active_indicator_bar), content.getChildAt(0));
        Assert.assertSame(reconnect(row), content.getChildAt(1));
        Assert.assertSame(row.findViewById(R.id.session_disable_toggle), content.getChildAt(2));
        Assert.assertSame(textColumn(row), content.getChildAt(3));
        ViewGroup.MarginLayoutParams reconnectParams = (ViewGroup.MarginLayoutParams)
            reconnect(row).getLayoutParams();
        Assert.assertEquals(dp(row, 0), reconnectParams.getMarginStart());
        Assert.assertEquals(dp(row, 2), reconnectParams.getMarginEnd());
        ViewGroup.MarginLayoutParams toggleParams = (ViewGroup.MarginLayoutParams)
            row.findViewById(R.id.session_disable_toggle).getLayoutParams();
        Assert.assertEquals(dp(row, 6), toggleParams.getMarginStart());
        Assert.assertEquals(dp(row, 16), toggleParams.getMarginEnd());
    }

    private static ViewGroup contentRow(View row) {
        return (ViewGroup) textColumn(row).getParent();
    }

    private static View textColumn(View row) {
        View title = row.findViewById(R.id.session_title);
        return (View) title.getParent();
    }

    private static View reconnect(View row) {
        return row.findViewById(R.id.session_reconnecting_indicator).getParent();
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static View inflateSessionRow() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context).inflate(R.layout.item_terminal_sessions_list,
            new FrameLayout(context), false);
    }

    private static final class Snapshot {
        private final View[] children;
        private final int reconnectStart;
        private final int reconnectEnd;
        private final int toggleStart;
        private final int toggleEnd;

        private Snapshot(View row) {
            ViewGroup content = contentRow(row);
            children = new View[content.getChildCount()];
            for (int index = 0; index < children.length; index++) {
                children[index] = content.getChildAt(index);
            }
            ViewGroup.MarginLayoutParams reconnectParams = (ViewGroup.MarginLayoutParams)
                reconnect(row).getLayoutParams();
            reconnectStart = reconnectParams.getMarginStart();
            reconnectEnd = reconnectParams.getMarginEnd();
            ViewGroup.MarginLayoutParams toggleParams = (ViewGroup.MarginLayoutParams)
                row.findViewById(R.id.session_disable_toggle).getLayoutParams();
            toggleStart = toggleParams.getMarginStart();
            toggleEnd = toggleParams.getMarginEnd();
        }

        private static Snapshot capture(View row) {
            return new Snapshot(row);
        }

        private void assertMatches(View row) {
            ViewGroup content = contentRow(row);
            for (int index = 0; index < children.length; index++) {
                Assert.assertSame(children[index], content.getChildAt(index));
            }
            ViewGroup.MarginLayoutParams reconnectParams = (ViewGroup.MarginLayoutParams)
                reconnect(row).getLayoutParams();
            Assert.assertEquals(reconnectStart, reconnectParams.getMarginStart());
            Assert.assertEquals(reconnectEnd, reconnectParams.getMarginEnd());
            ViewGroup.MarginLayoutParams toggleParams = (ViewGroup.MarginLayoutParams)
                row.findViewById(R.id.session_disable_toggle).getLayoutParams();
            Assert.assertEquals(toggleStart, toggleParams.getMarginStart());
            Assert.assertEquals(toggleEnd, toggleParams.getMarginEnd());
        }
    }
}
