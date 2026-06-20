package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellTwoTimestampBehaviorTest {

    private static SessionNewActivityIndicator indicatorFor(SessionNewActivityStore store,
                                                            String handle, long nowMillis) {
        return TermuxSessionsListViewController.newActivityIndicator(store, handle, nowMillis);
    }

    @Test
    public void realBellInANonViewedSessionShowsTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background-handle", 1_000L);

        Assert.assertTrue(indicatorFor(store, "background-handle", 2_000L).isVisible());
    }

    @Test
    public void ordinaryOutputDoesNotSetTheBellSoNoZeroSecondsAgoForever() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordSeen("background-handle", 1_000L);

        Assert.assertNull(store.getLastBellTimeMillis("background-handle"));
        Assert.assertFalse(indicatorFor(store, "background-handle", 2_000L).isVisible());
        Assert.assertFalse(indicatorFor(store, "background-handle", 60_000L).isVisible());
    }

    @Test
    public void viewingASessionDoesNotClearTheBellInstantlyBeforeTheDwellElapses() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("viewed-handle", 5_000L);

        long switchMomentMillis = 5_100L;
        Assert.assertTrue(indicatorFor(store, "viewed-handle", switchMomentMillis).isVisible());
    }

    @Test
    public void viewingASessionClearsTheBellOnceTheSeenTimeAdvancesPastTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("viewed-handle", 5_000L);

        store.recordSeen("viewed-handle", 6_100L);

        Assert.assertFalse(indicatorFor(store, "viewed-handle", 6_200L).isVisible());
    }

    @Test
    public void aBellArrivingAfterTheSessionWasSeenReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle", 1_000L);
        store.recordSeen("handle", 2_000L);
        Assert.assertFalse(indicatorFor(store, "handle", 2_500L).isVisible());

        store.recordBell("handle", 9_000L);

        Assert.assertTrue(indicatorFor(store, "handle", 9_500L).isVisible());
    }

    @Test
    public void ageLabelAdvancesOverTimeWhileTheBellStaysUnseen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle", 1_000L);

        String labelAtFiveSeconds = indicatorFor(store, "handle", 6_000L).getLabel();
        String labelAtFortySeconds = indicatorFor(store, "handle", 41_000L).getLabel();

        Assert.assertEquals("5s ago", labelAtFiveSeconds);
        Assert.assertEquals("40s ago", labelAtFortySeconds);
    }

    @Test
    public void purgingARemovedSessionDropsItsBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle", 1_000L);

        store.purgeSession("handle");

        Assert.assertFalse(indicatorFor(store, "handle", 2_000L).isVisible());
    }
}
