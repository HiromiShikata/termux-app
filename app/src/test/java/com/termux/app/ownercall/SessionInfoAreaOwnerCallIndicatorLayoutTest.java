package com.termux.app.ownercall;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionInfoAreaOwnerCallIndicatorLayoutTest {

    private static final int SESSION_AREA_WIDTH_PIXELS = 1080;

    @Test
    public void theOwnerCallIndicatorAddsNoHeightToTheSessionInformationArea() {
        int heightWithoutTheIndicator = sessionInformationAreaHeight(View.GONE);
        int heightWithTheIndicator = sessionInformationAreaHeight(View.VISIBLE);

        Assert.assertEquals("the owner call indicator must not cost a row of vertical space",
            heightWithoutTheIndicator, heightWithTheIndicator);
    }

    @Test
    public void theOwnerCallIndicatorSitsAtTheRightHandEndOfTheSessionInformationArea() {
        View root = inflateActivityLayout();
        View sessionInformationArea = layOutSessionInformationArea(root, View.VISIBLE);
        View indicator = root.findViewById(R.id.owner_call_pending_indicator);

        Assert.assertEquals("the indicator must end at the right-hand end of the session area",
            sessionInformationArea.getWidth(), indicator.getRight());
        Assert.assertTrue("the session bars must keep the width the indicator leaves",
            root.findViewById(R.id.session_name_bar).getRight() <= indicator.getLeft());
    }

    private static int sessionInformationAreaHeight(int indicatorVisibility) {
        return layOutSessionInformationArea(inflateActivityLayout(), indicatorVisibility)
            .getHeight();
    }

    private static View layOutSessionInformationArea(View root, int indicatorVisibility) {
        root.findViewById(R.id.session_project_story_bar).setVisibility(View.VISIBLE);
        root.findViewById(R.id.session_name_bar).setVisibility(View.VISIBLE);
        root.findViewById(R.id.session_last_reply_bar).setVisibility(View.VISIBLE);
        root.findViewById(R.id.owner_call_pending_indicator).setVisibility(indicatorVisibility);

        View sessionInformationArea = root.findViewById(R.id.session_info_bottom_container);
        sessionInformationArea.measure(
            View.MeasureSpec.makeMeasureSpec(SESSION_AREA_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        sessionInformationArea.layout(0, 0, sessionInformationArea.getMeasuredWidth(),
            sessionInformationArea.getMeasuredHeight());
        return sessionInformationArea;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
