package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellTwoTimestampBehaviorTest {

    private static SessionNewActivityIndicator indicatorFor(SessionNewActivityStore store,
                                                            String sessionName, long nowMillis) {
        return TermuxSessionsListViewController.newActivityIndicator(store, sessionName, nowMillis);
    }

    @Test
    public void explicitCallInANonViewedSessionShowsRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("background", 1_000L);

        SessionNewActivityIndicator indicator = indicatorFor(store, "background", 2_000L);
        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.RED, indicator.getTier());
    }

    @Test
    public void outputActivityInANonViewedSessionShowsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("background", 1_000L);

        SessionNewActivityIndicator indicator = indicatorFor(store, "background", 2_000L);
        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
    }

    @Test
    public void seenAloneNeverProducesAnIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordSeen("background", 1_000L);

        Assert.assertNull(store.getLastExplicitCallTimeMillis("background"));
        Assert.assertNull(store.getLastOutputActivityTimeMillis("background"));
        Assert.assertFalse(indicatorFor(store, "background", 2_000L).isVisible());
        Assert.assertFalse(indicatorFor(store, "background", 60_000L).isVisible());
    }

    @Test
    public void viewingASessionDoesNotClearTheSignalBeforeSeenTimeAdvances() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("viewed", 5_000L);

        long switchMomentMillis = 5_100L;
        Assert.assertTrue(indicatorFor(store, "viewed", switchMomentMillis).isVisible());
    }

    @Test
    public void viewingASessionClearsTheSignalOnceTheSeenTimeAdvancesPastIt() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("viewed", 5_000L);

        store.recordSeen("viewed", 6_100L);

        Assert.assertFalse(indicatorFor(store, "viewed", 6_200L).isVisible());
    }

    @Test
    public void aSignalArrivingAfterTheSessionWasSeenReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session", 1_000L);
        store.recordSeen("session", 2_000L);
        Assert.assertFalse(indicatorFor(store, "session", 2_500L).isVisible());

        store.recordExplicitCall("session", 9_000L);

        Assert.assertTrue(indicatorFor(store, "session", 9_500L).isVisible());
    }

    @Test
    public void ageLabelAdvancesOverTimeWhileTheSignalStaysUnseen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session", 1_000L);

        String labelAtFiveSeconds = indicatorFor(store, "session", 6_000L).getLabel();
        String labelAtFortySeconds = indicatorFor(store, "session", 41_000L).getLabel();

        Assert.assertEquals("5s ago", labelAtFiveSeconds);
        Assert.assertEquals("40s ago", labelAtFortySeconds);
    }

    @Test
    public void purgingARemovedSessionDropsItsSignals() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session", 1_000L);

        store.purgeSession("session");

        Assert.assertFalse(indicatorFor(store, "session", 2_000L).isVisible());
    }
}
