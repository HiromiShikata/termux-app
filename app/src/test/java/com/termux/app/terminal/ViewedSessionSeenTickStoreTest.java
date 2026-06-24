package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ViewedSessionSeenTickStoreTest {

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
    public void viewedSessionSeenTickAdvancesEverySecondUnconditionallyAndClearsItsOwnCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        // While the user is genuinely viewing the session, its per-second seen tick advances seen
        // unconditionally (no explicit-call suppression). A call arriving while the user is present
        // is therefore treated as seen and leaves no leftover indicator after the user switches away.
        // This mirrors TermuxTerminalSessionActivityClient.recordActiveSessionSeen, which records the
        // current session's seen on every tick without any pending-call guard.
        for (long nowMillis = 3_000L; nowMillis <= 60_000L; nowMillis += 1_000L) {
            store.recordSeen(CALLED_SESSION, nowMillis);
        }

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void outputArrivingInTheViewedSessionIsSeenImmediatelyAndLeavesNoIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        // Genuine output of the currently-viewed session records output activity and, because it is
        // the viewed session, also records seen at the same instant. The call and the output that
        // arrived during the stay are both seen, so no indicator is left when the user switches away.
        long outputMillis = 4_000L;
        store.recordOutputActivity(CALLED_SESSION, outputMillis);
        store.recordSeen(CALLED_SESSION, outputMillis);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void nonViewedCalledSessionSeenNeverAdvancesSoItsRedDotPersistsUntilViewed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String activeSession = OTHER_SESSION;
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        // Only the viewed (active) session's seen ticks; a non-viewed session's seen is never
        // recorded by the tick, so its red dot persists.
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
