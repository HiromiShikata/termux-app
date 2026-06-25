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

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class PendingCallToUserFooterBinderTest {

    @Test
    public void footerBarAndScrollButtonExistInTheBottomSessionInfoContainerAndStartHidden() {
        View root = inflateActivityLayout();

        View bar = root.findViewById(R.id.session_pending_call_to_user_bar);
        View container = root.findViewById(R.id.session_info_bottom_container);
        TextView text = root.findViewById(R.id.session_pending_call_to_user_text);
        ImageButton scrollButton = root.findViewById(R.id.session_pending_call_to_user_scroll_button);

        Assert.assertNotNull(bar);
        Assert.assertNotNull(text);
        Assert.assertNotNull(scrollButton);
        Assert.assertTrue(isDescendantOf(bar, container));
        Assert.assertEquals(View.GONE, bar.getVisibility());
    }

    @Test
    public void bindShowsReasonTextAndWiresScrollActionWhenCallToUserIsPending() {
        View root = inflateActivityLayout();
        View bar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView text = root.findViewById(R.id.session_pending_call_to_user_text);
        ImageButton scrollButton = root.findViewById(R.id.session_pending_call_to_user_scroll_button);
        AtomicInteger scrollInvocations = new AtomicInteger(0);

        PendingCallToUserFooterBinder.bind(bar, text, scrollButton,
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, "needs approval"),
            scrollInvocations::incrementAndGet);

        Assert.assertEquals(View.VISIBLE, bar.getVisibility());
        Assert.assertEquals("needs approval", text.getText().toString());
        Assert.assertTrue(scrollButton.hasOnClickListeners());

        scrollButton.performClick();

        Assert.assertEquals(1, scrollInvocations.get());
    }

    @Test
    public void bindHidesBarAndClearsActionWhenCallToUserIsAnswered() {
        View root = inflateActivityLayout();
        View bar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView text = root.findViewById(R.id.session_pending_call_to_user_text);
        ImageButton scrollButton = root.findViewById(R.id.session_pending_call_to_user_scroll_button);
        AtomicInteger scrollInvocations = new AtomicInteger(0);

        PendingCallToUserFooterBinder.bind(bar, text, scrollButton,
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.RED, "needs approval"),
            scrollInvocations::incrementAndGet);
        PendingCallToUserFooterBinder.bind(bar, text, scrollButton,
            PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.NONE, "needs approval"),
            scrollInvocations::incrementAndGet);

        Assert.assertEquals(View.GONE, bar.getVisibility());
        Assert.assertEquals("", text.getText().toString());
        Assert.assertFalse(scrollButton.hasOnClickListeners());

        scrollButton.performClick();

        Assert.assertEquals(0, scrollInvocations.get());
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
