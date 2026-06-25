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
    public void viewingASessionDoesNotClearTheRedCallEvenAfterTheSeenTimeAdvancesPastIt() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("viewed", 5_000L);

        store.recordSeen("viewed", 6_100L);

        Assert.assertTrue(indicatorFor(store, "viewed", 6_200L).isVisible());
        Assert.assertEquals(SessionNewActivityTier.RED, indicatorFor(store, "viewed", 6_200L).getTier());
    }

    @Test
    public void aCallArrivingAfterTheUserRepliedReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session", 1_000L);
        store.recordUserInput("session", 2_000L);
        Assert.assertFalse(indicatorFor(store, "session", 2_500L).isVisible());

        store.recordExplicitCall("session", 9_000L);

        Assert.assertTrue(indicatorFor(store, "session", 9_500L).isVisible());
        Assert.assertEquals(SessionNewActivityTier.RED, indicatorFor(store, "session", 9_500L).getTier());
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
    public void recentOutputInTheViewedSessionStaysYellowBecauseYellowDoesNotClearOnView() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long viewedOutputMillis = 1_000L;
        store.recordOutputActivity("viewed", viewedOutputMillis);
        store.recordSeen("viewed", viewedOutputMillis);

        Assert.assertEquals(SessionNewActivityTier.YELLOW,
            indicatorFor(store, "viewed", 1_500L).getTier());
        Assert.assertEquals(SessionNewActivityTier.YELLOW,
            indicatorFor(store, "viewed", 60_000L).getTier());
    }

    @Test
    public void idleOutputOlderThanTenMinutesInTheViewedSessionShowsGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long viewedOutputMillis = 1_000L;
        store.recordOutputActivity("viewed", viewedOutputMillis);
        store.recordSeen("viewed", viewedOutputMillis);

        long elevenMinutesLater = viewedOutputMillis + 11L * 60L * 1000L;
        Assert.assertEquals(SessionNewActivityTier.GRAY,
            indicatorFor(store, "viewed", elevenMinutesLater).getTier());
    }

    @Test
    public void recordedOutputActivityNeverProducesRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicatorFor(store, "session", 2_000L).getTier());
    }

    @Test
    public void purgingARemovedSessionDropsItsSignals() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session", 1_000L);

        store.purgeSession("session");

        Assert.assertFalse(indicatorFor(store, "session", 2_000L).isVisible());
    }
}
