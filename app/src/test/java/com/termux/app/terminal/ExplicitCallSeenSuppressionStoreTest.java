package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ExplicitCallSeenSuppressionStoreTest {

    private static final String CALLED_SESSION = "called-session";
    private static final String OTHER_SESSION = "other-session";

    @Test
    public void pendingExplicitCallIsReportedWhenNeverSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        Assert.assertTrue(store.hasPendingExplicitCall(CALLED_SESSION));
    }

    @Test
    public void pendingExplicitCallIsReportedWhenCallIsNewerThanSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(CALLED_SESSION, 1_000L);
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        Assert.assertTrue(store.hasPendingExplicitCall(CALLED_SESSION));
    }

    @Test
    public void noPendingExplicitCallWhenSeenIsNewerThanCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");
        store.recordSeen(CALLED_SESSION, 3_000L);

        Assert.assertFalse(store.hasPendingExplicitCall(CALLED_SESSION));
    }

    @Test
    public void noPendingExplicitCallForOutputOnlySession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(CALLED_SESSION, 2_000L);

        Assert.assertFalse(store.hasPendingExplicitCall(CALLED_SESSION));
    }

    @Test
    public void activeSessionSeenTickAdvancesSeenUnconditionallyAndClearsRedWithinASecond() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        // The viewed session's per-second seen tick records seen as now without any
        // explicit-call suppression, so the red tier clears on the next tick.
        store.recordSeen(CALLED_SESSION, 3_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void backgroundCalledSessionWhoseSeenIsNeverTickedStaysRedUntilViewed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String activeSession = OTHER_SESSION;
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        // Only the active session's seen ticks; the background called session's seen
        // is never recorded by the tick, so it stays red.
        for (long nowMillis = 3_000L; nowMillis <= 30_000L; nowMillis += 1_000L) {
            store.recordSeen(activeSession, nowMillis);
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));

        // When the user switches to and views the called session, its seen advances and red clears.
        store.recordSeen(CALLED_SESSION, 31_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void genuineSwitchToCalledSessionAcknowledgesAndClearsRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        if (store.hasPendingExplicitCall(CALLED_SESSION)) {
            store.recordSeen(CALLED_SESSION, 5_000L);
        }

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void automaticTickClearsYellowWhenNoExplicitCallPending() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(OTHER_SESSION, 1_000L);
        store.recordOutputActivity(OTHER_SESSION, 1_400L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(OTHER_SESSION));

        store.recordSeen(OTHER_SESSION, 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(OTHER_SESSION));
    }
}
