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
public class SessionListLeftHandedTest {

    @Test
    public void leftHandedSessionListMovesToggleFirstAndRightAlignsTitle() {
        View root = inflateActivityLayout();
        new SessionListHandednessPart(root).applyHandedness(Handedness.LEFT_HANDED);

        TextView title = root.findViewById(R.id.session_list_bottom_sheet_title);
        LinearLayout titleRow = (LinearLayout) title.getParent();
        Assert.assertSame(root.findViewById(R.id.session_list_bottom_sheet_hidden_toggle_button),
            titleRow.getChildAt(0));
        Assert.assertSame(title, titleRow.getChildAt(1));
        Assert.assertEquals(android.view.Gravity.END | android.view.Gravity.TOP, title.getGravity());

        LinearLayout controls = root.findViewById(R.id.session_list_bottom_sheet_controls_group);
        assertChildren(controls,
            R.id.session_list_bottom_sheet_google_button,
            R.id.session_list_bottom_sheet_load_session_button,
            R.id.session_list_bottom_sheet_new_session_button,
            R.id.session_list_bottom_sheet_settings_button);
        Assert.assertEquals(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL,
            controls.getGravity());
    }

    @Test
    public void leftHandedSessionListThenRightHandedRestoresThePinnedState() {
        View root = inflateActivityLayout();
        Snapshot before = Snapshot.capture(root);

        SessionListHandednessPart part = new SessionListHandednessPart(root);
        part.applyHandedness(Handedness.LEFT_HANDED);
        part.applyHandedness(Handedness.RIGHT_HANDED);

        before.assertMatches(root);
    }

    @Test
    public void applyingLeftHandedSessionListTwiceIsIdempotent() {
        View root = inflateActivityLayout();
        SessionListHandednessPart part = new SessionListHandednessPart(root);
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

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

    private static final class Snapshot {
        private final View title;
        private final View titleRowFirst;
        private final View titleRowSecond;
        private final int titleGravity;
        private final View[] controls;
        private final int controlsGravity;

        private Snapshot(View root) {
            title = root.findViewById(R.id.session_list_bottom_sheet_title);
            LinearLayout titleRow = (LinearLayout) title.getParent();
            titleRowFirst = titleRow.getChildAt(0);
            titleRowSecond = titleRow.getChildAt(1);
            titleGravity = ((TextView) title).getGravity();
            LinearLayout controls = root.findViewById(R.id.session_list_bottom_sheet_controls_group);
            controlsGravity = controls.getGravity();
            this.controls = new View[controls.getChildCount()];
            for (int index = 0; index < controls.getChildCount(); index++) {
                this.controls[index] = controls.getChildAt(index);
            }
        }

        private static Snapshot capture(View root) {
            return new Snapshot(root);
        }

        private void assertMatches(View root) {
            TextView currentTitle = root.findViewById(R.id.session_list_bottom_sheet_title);
            LinearLayout titleRow = (LinearLayout) currentTitle.getParent();
            Assert.assertSame(titleRowFirst, titleRow.getChildAt(0));
            Assert.assertSame(titleRowSecond, titleRow.getChildAt(1));
            Assert.assertEquals(titleGravity, currentTitle.getGravity());
            LinearLayout currentControls = root.findViewById(
                R.id.session_list_bottom_sheet_controls_group);
            Assert.assertEquals(controls.length, currentControls.getChildCount());
            for (int index = 0; index < controls.length; index++) {
                Assert.assertSame(controls[index], currentControls.getChildAt(index));
            }
            Assert.assertEquals(controlsGravity, currentControls.getGravity());
        }
    }
}
