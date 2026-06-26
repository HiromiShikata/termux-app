package com.termux.app.terminal;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class SessionInfoBottomBarsCombinedTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long NOW = 1_000_000_000L;
    private static final int WIDTH_PIXELS = 300;

    @Test
    public void timesLineAndCallToUserSceneAreBothVisibleInTheSameBottomContainer() {
        View root = inflateActivityLayout();
        TextView timesBar = root.findViewById(R.id.session_last_reply_bar);
        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);
        View bottomContainer = root.findViewById(R.id.session_info_bottom_container);

        bindTimesLine(timesBar, SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 45L * ONE_SECOND_MILLIS,
            NOW));
        bindCallToUserScene(root, PendingCallToUserFooterDecision.resolveAll(
            SessionNewActivityTier.RED,
            Arrays.asList("needs approval to deploy", "waiting for the secret value")));

        Assert.assertTrue(isDescendantOf(timesBar, bottomContainer));
        Assert.assertTrue(isDescendantOf(sceneBar, bottomContainer));
        Assert.assertEquals(View.VISIBLE, timesBar.getVisibility());
        Assert.assertEquals(View.VISIBLE, sceneBar.getVisibility());
        Assert.assertEquals("call: 3h  out: 12m  reply: 45s", timesBar.getText().toString());
        Assert.assertEquals("needs approval to deploy\nwaiting for the secret value",
            sceneText.getText().toString());
    }

    @Test
    public void timesLineRendersAboveTheCallToUserSceneWithoutDuplication() {
        View root = inflateActivityLayout();
        TextView timesBar = root.findViewById(R.id.session_last_reply_bar);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);
        View sceneBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        View bottomContainer = root.findViewById(R.id.session_info_bottom_container);

        bindTimesLine(timesBar, SessionTimesLine.of(
            NOW - 5L * ONE_MINUTE_MILLIS, NOW - 5L * ONE_MINUTE_MILLIS, NOW - 5L * ONE_MINUTE_MILLIS,
            NOW));
        bindCallToUserScene(root, PendingCallToUserFooterDecision.resolveAll(
            SessionNewActivityTier.RED, Arrays.asList("approve the pull request merge")));

        int timesIndex = childIndexUnder(bottomContainer, timesBar);
        int sceneIndex = childIndexUnder(bottomContainer, sceneBar);
        Assert.assertTrue("times line must render above the call-to-user scene",
            timesIndex >= 0 && sceneIndex >= 0 && timesIndex < sceneIndex);
        Assert.assertNotEquals("the scene must hold the call-to-user content, not the times text",
            timesBar.getText().toString(), sceneText.getText().toString());
        Assert.assertEquals("approve the pull request merge", sceneText.getText().toString());
    }

    @Test
    public void callToUserSceneStaysHeightCappedWhileTheTimesLineIsAlsoShown() {
        View root = inflateActivityLayout();
        TextView timesBar = root.findViewById(R.id.session_last_reply_bar);
        MaxHeightScrollView sceneScroll =
            root.findViewById(R.id.session_pending_call_to_user_scroll);
        TextView sceneText = root.findViewById(R.id.session_pending_call_to_user_text);

        StringBuilder hugeScene = new StringBuilder();
        for (int entry = 0; entry < 200; entry++) {
            hugeScene.append("call-to-user reason number ").append(entry)
                .append(" that wraps across several lines of the footer\n");
        }
        bindTimesLine(timesBar, SessionTimesLine.of(NOW - ONE_MINUTE_MILLIS, NOW, NOW, NOW));
        bindCallToUserScene(root, PendingCallToUserFooterDecision.resolveAll(
            SessionNewActivityTier.RED, Arrays.asList(hugeScene.toString())));

        Assert.assertEquals(View.VISIBLE, timesBar.getVisibility());
        Assert.assertTrue(sceneScroll.getMaxScrollHeightPixels() > 0);

        sceneScroll.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        sceneScroll.layout(0, 0, sceneScroll.getMeasuredWidth(), sceneScroll.getMeasuredHeight());

        Assert.assertEquals(sceneScroll.getMaxScrollHeightPixels(), sceneScroll.getMeasuredHeight());
        Assert.assertTrue("the scene content must exceed the capped footer height",
            sceneScroll.getChildAt(0).getMeasuredHeight() > sceneScroll.getMeasuredHeight());
        Assert.assertEquals(Integer.MAX_VALUE, sceneText.getMaxLines());
    }

    private static void bindTimesLine(TextView timesBar, SessionTimesLine line) {
        if (line.isVisible()) {
            timesBar.setText(line.getText());
            timesBar.setVisibility(View.VISIBLE);
        } else {
            timesBar.setText("");
            timesBar.setVisibility(View.GONE);
        }
    }

    private static void bindCallToUserScene(View root, PendingCallToUserFooterDecision decision) {
        View bar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView text = root.findViewById(R.id.session_pending_call_to_user_text);
        ImageButton scrollButton =
            root.findViewById(R.id.session_pending_call_to_user_scroll_button);
        PendingCallToUserFooterBinder.bind(bar, text, scrollButton, decision, () -> {
        });
    }

    private static int childIndexUnder(View container, View descendant) {
        View current = descendant;
        while (current != null && current.getParent() != container) {
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                return -1;
            }
        }
        if (current == null || !(container instanceof android.view.ViewGroup)) {
            return -1;
        }
        return ((android.view.ViewGroup) container).indexOfChild(current);
    }

    private static boolean isDescendantOf(View view, View ancestor) {
        View current = view;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                return false;
            }
        }
        return false;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
