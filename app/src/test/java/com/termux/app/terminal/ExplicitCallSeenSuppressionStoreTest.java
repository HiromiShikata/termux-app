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
    public void backgroundRecordedCallSurvivesTheAutomaticSeenTickAndStaysRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "needs approval");

        for (long nowMillis = 3_000L; nowMillis <= 30_000L; nowMillis += 1_000L) {
            if (!store.hasPendingExplicitCall(CALLED_SESSION)) {
                store.recordSeen(CALLED_SESSION, nowMillis);
            }
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void completionMarkerRecordedCallSurvivesTheAutomaticSeenTickAndStaysRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(CALLED_SESSION, 1_000L);
        store.recordExplicitCall(CALLED_SESSION, 2_000L, "done");

        for (long nowMillis = 2_500L; nowMillis <= 30_000L; nowMillis += 500L) {
            if (!store.hasPendingExplicitCall(CALLED_SESSION)) {
                store.recordSeen(CALLED_SESSION, nowMillis);
            }
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
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
    public void automaticTickStillClearsYellowWhenNoExplicitCallPending() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(OTHER_SESSION, 1_000L);
        store.recordOutputActivity(OTHER_SESSION, 1_400L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(OTHER_SESSION));

        if (!store.hasPendingExplicitCall(OTHER_SESSION)) {
            store.recordSeen(OTHER_SESSION, 2_000L);
        }

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(OTHER_SESSION));
    }
}
