package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellTwoTimestampBehaviorTest {

    private static SessionNewActivityIndicator indicatorFor(SessionNewActivityStore store,
                                                            String sessionName, long nowMillis) {
        return TermuxSessionsListViewController.newActivityIndicator(store, sessionName, nowMillis);
    }

    @Test
    public void realBellInANonViewedSessionShowsTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("background", 1_000L);

        Assert.assertTrue(indicatorFor(store, "background", 2_000L).isVisible());
    }

    @Test
    public void ordinaryOutputDoesNotSetTheBellSoNoZeroSecondsAgoForever() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordSeen("background", 1_000L);

        Assert.assertNull(store.getLastBellTimeMillis("background"));
        Assert.assertFalse(indicatorFor(store, "background", 2_000L).isVisible());
        Assert.assertFalse(indicatorFor(store, "background", 60_000L).isVisible());
    }

    @Test
    public void viewingASessionDoesNotClearTheBellInstantlyBeforeTheDwellElapses() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("viewed", 5_000L);

        long switchMomentMillis = 5_100L;
        Assert.assertTrue(indicatorFor(store, "viewed", switchMomentMillis).isVisible());
    }

    @Test
    public void viewingASessionClearsTheBellOnceTheSeenTimeAdvancesPastTheBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("viewed", 5_000L);

        store.recordSeen("viewed", 6_100L);

        Assert.assertFalse(indicatorFor(store, "viewed", 6_200L).isVisible());
    }

    @Test
    public void aBellArrivingAfterTheSessionWasSeenReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session", 1_000L);
        store.recordSeen("session", 2_000L);
        Assert.assertFalse(indicatorFor(store, "session", 2_500L).isVisible());

        store.recordBell("session", 9_000L);

        Assert.assertTrue(indicatorFor(store, "session", 9_500L).isVisible());
    }

    @Test
    public void ageLabelAdvancesOverTimeWhileTheBellStaysUnseen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session", 1_000L);

        String labelAtFiveSeconds = indicatorFor(store, "session", 6_000L).getLabel();
        String labelAtFortySeconds = indicatorFor(store, "session", 41_000L).getLabel();

        Assert.assertEquals("5s ago", labelAtFiveSeconds);
        Assert.assertEquals("40s ago", labelAtFortySeconds);
    }

    @Test
    public void purgingARemovedSessionDropsItsBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session", 1_000L);

        store.purgeSession("session");

        Assert.assertFalse(indicatorFor(store, "session", 2_000L).isVisible());
    }
}
