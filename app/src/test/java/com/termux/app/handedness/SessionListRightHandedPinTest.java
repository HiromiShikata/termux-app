package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class SessionListRightHandedPinTest {

    @Test
    public void sessionListTitleKeepsTitleBeforeHiddenSessionsToggleAndStartGravity() {
        View root = inflateActivityLayout();
        new SessionListHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);

        TextView title = root.findViewById(R.id.session_list_bottom_sheet_title);
        LinearLayout row = (LinearLayout) title.getParent();

        Assert.assertSame(title, row.getChildAt(0));
        Assert.assertSame(root.findViewById(R.id.session_list_bottom_sheet_hidden_toggle_button),
            row.getChildAt(1));
        Assert.assertEquals(android.view.Gravity.START, title.getGravity());
    }

    @Test
    public void sessionListControlsKeepCurrentOrderAndEndCenterGravity() {
        View root = inflateActivityLayout();
        new SessionListHandednessPart(root).applyHandedness(Handedness.RIGHT_HANDED);

        LinearLayout controls = root.findViewById(R.id.session_list_bottom_sheet_controls_group);
        assertChildren(controls,
            R.id.session_list_bottom_sheet_settings_button,
            R.id.session_list_bottom_sheet_new_session_button,
            R.id.session_list_bottom_sheet_load_session_button,
            R.id.session_list_bottom_sheet_google_button);
        Assert.assertEquals(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL,
            controls.getGravity());
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
}
