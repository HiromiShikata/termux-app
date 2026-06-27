package com.termux.app.terminal;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;

public class SessionActivityTierDotRenderingTest {

    @Test
    public void redTierSelectsRedDotDrawable() {
        Assert.assertEquals(R.drawable.ic_session_activity_dot_red,
            SessionActivityTierDrawables.dotDrawableRes(SessionNewActivityTier.RED));
    }

    @Test
    public void yellowTierSelectsYellowDotDrawable() {
        Assert.assertEquals(R.drawable.ic_session_activity_dot_yellow,
            SessionActivityTierDrawables.dotDrawableRes(SessionNewActivityTier.YELLOW));
    }

    @Test
    public void noneTierSelectsPlaceholderDotDrawable() {
        Assert.assertEquals(R.drawable.ic_session_activity_dot_placeholder,
            SessionActivityTierDrawables.dotDrawableRes(SessionNewActivityTier.NONE));
    }

    @Test
    public void controllerMapsRedTierToRedDotDrawable() {
        Assert.assertEquals(R.drawable.ic_session_activity_dot_red,
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(SessionNewActivityTier.RED));
    }

    @Test
    public void recordedExplicitCallNewerThanLastSeenResolvesToRedAndSelectsRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("agent", 1_000L);
        store.recordExplicitCall("agent", 5_000L, "needs approval");

        SessionNewActivityTier tier = store.tierFor("agent");

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_red,
            SessionActivityTierDrawables.dotDrawableRes(tier));
    }

    @Test
    public void genuineNewOutputNewerThanLastSeenResolvesToYellowAndSelectsYellowDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("agent", 1_000L);
        store.recordOutputActivity("agent", 5_000L);

        SessionNewActivityTier tier = store.tierFor("agent");

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_yellow,
            SessionActivityTierDrawables.dotDrawableRes(tier));
    }

    @Test
    public void sessionWithNoOutputResolvesToNoneAndSelectsPlaceholderDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        SessionNewActivityTier tier = store.tierFor("agent");

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_placeholder,
            SessionActivityTierDrawables.dotDrawableRes(tier));
    }

    @Test
    public void activeSessionKeptSeenByTickResolvesToNoneAndSelectsPlaceholderDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("agent", 4_000L);
        store.recordSeen("agent", 4_000L);

        SessionNewActivityTier tier = store.tierFor("agent");

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_placeholder,
            SessionActivityTierDrawables.dotDrawableRes(tier));
    }
}
