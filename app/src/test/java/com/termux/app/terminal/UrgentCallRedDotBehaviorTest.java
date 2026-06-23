package com.termux.app.terminal;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;

public class UrgentCallRedDotBehaviorTest {

    private static final String URGENT_SESSION = "urgent-session";
    private static final String OTHER_SESSION = "other-session";

    @Test
    public void suppressesSeenForTheUrgentlyDisplayedSession() {
        Assert.assertTrue(
            UrgentCallSeenSuppression.shouldSuppressSeen(URGENT_SESSION, URGENT_SESSION));
    }

    @Test
    public void doesNotSuppressSeenForAnUnrelatedActiveSession() {
        Assert.assertFalse(
            UrgentCallSeenSuppression.shouldSuppressSeen(URGENT_SESSION, OTHER_SESSION));
    }

    @Test
    public void doesNotSuppressSeenWhenNothingIsPending() {
        Assert.assertFalse(
            UrgentCallSeenSuppression.shouldSuppressSeen(null, URGENT_SESSION));
    }

    @Test
    public void switchingToADifferentSessionClearsTheSuppression() {
        Assert.assertTrue(
            UrgentCallSeenSuppression.clearsSuppressionOnSwitch(URGENT_SESSION, OTHER_SESSION));
    }

    @Test
    public void displayingTheUrgentSessionItselfKeepsTheSuppression() {
        Assert.assertFalse(
            UrgentCallSeenSuppression.clearsSuppressionOnSwitch(URGENT_SESSION, URGENT_SESSION));
    }

    @Test
    public void recordedUrgentCallProducesVisibleRedDotAfterUserSwitchesAway() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(URGENT_SESSION, 1_000L);

        String suppressedSessionName = null;

        store.recordExplicitCall(URGENT_SESSION, 2_000L);
        suppressedSessionName = URGENT_SESSION;

        for (long nowMillis = 2_000L; nowMillis <= 10_000L; nowMillis += 1_000L) {
            if (!UrgentCallSeenSuppression.shouldSuppressSeen(suppressedSessionName, URGENT_SESSION)) {
                store.recordSeen(URGENT_SESSION, nowMillis);
            }
        }

        if (UrgentCallSeenSuppression.clearsSuppressionOnSwitch(suppressedSessionName, OTHER_SESSION)) {
            suppressedSessionName = null;
        }
        store.recordSeen(OTHER_SESSION, 11_000L);

        SessionNewActivityTier tier = store.tierFor(URGENT_SESSION);
        Assert.assertEquals(SessionNewActivityTier.RED, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_red,
            SessionActivityTierDrawables.dotDrawableRes(tier));
    }

    @Test
    public void withoutSuppressionTheAutoDisplaySeenClobbersTheCallAndRedNeverShows() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(URGENT_SESSION, 1_000L);

        store.recordExplicitCall(URGENT_SESSION, 2_000L);

        for (long nowMillis = 2_000L; nowMillis <= 10_000L; nowMillis += 1_000L) {
            store.recordSeen(URGENT_SESSION, nowMillis);
        }

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(URGENT_SESSION));
    }

    @Test
    public void genuinelyViewingTheUrgentSessionAfterSwitchingBackClearsRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(URGENT_SESSION, 1_000L);

        String suppressedSessionName = null;
        store.recordExplicitCall(URGENT_SESSION, 2_000L);
        suppressedSessionName = URGENT_SESSION;

        if (UrgentCallSeenSuppression.clearsSuppressionOnSwitch(suppressedSessionName, OTHER_SESSION)) {
            suppressedSessionName = null;
        }
        store.recordSeen(OTHER_SESSION, 3_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(URGENT_SESSION));

        if (UrgentCallSeenSuppression.clearsSuppressionOnSwitch(suppressedSessionName, URGENT_SESSION)) {
            suppressedSessionName = null;
        }
        if (!UrgentCallSeenSuppression.shouldSuppressSeen(suppressedSessionName, URGENT_SESSION)) {
            store.recordSeen(URGENT_SESSION, 4_000L);
        }

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(URGENT_SESSION));
    }
}
