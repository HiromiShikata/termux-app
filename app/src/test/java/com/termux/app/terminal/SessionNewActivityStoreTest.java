package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStoreTest {

    @Test
    public void marksAndExposesArrivalTimeByHandle() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertFalse(store.hasNewActivity("handle-one"));
        Assert.assertNull(store.getArrivalTimeMillis("handle-one"));

        store.markNewActivity("handle-one", 1_000L);

        Assert.assertTrue(store.hasNewActivity("handle-one"));
        Assert.assertEquals(Long.valueOf(1_000L), store.getArrivalTimeMillis("handle-one"));
    }

    @Test
    public void clearRemovesTheEntry() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.markNewActivity("handle-one", 1_000L);

        store.clearNewActivity("handle-one");

        Assert.assertFalse(store.hasNewActivity("handle-one"));
        Assert.assertNull(store.getArrivalTimeMillis("handle-one"));
    }

    @Test
    public void purgeRemovesTheEntry() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.markNewActivity("handle-one", 1_000L);

        store.purgeSession("handle-one");

        Assert.assertFalse(store.hasNewActivity("handle-one"));
        Assert.assertNull(store.getArrivalTimeMillis("handle-one"));
    }

    @Test
    public void clearedEntryStaysClearedUntilANewMark() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.markNewActivity("handle-one", 1_000L);
        store.clearNewActivity("handle-one");
        Assert.assertFalse(store.hasNewActivity("handle-one"));

        store.markNewActivity("handle-one", 9_000L);

        Assert.assertTrue(store.hasNewActivity("handle-one"));
        Assert.assertEquals(Long.valueOf(9_000L), store.getArrivalTimeMillis("handle-one"));
    }

    @Test
    public void markingAgainForSameHandleUpdatesArrivalTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.markNewActivity("handle-one", 1_000L);
        store.markNewActivity("handle-one", 5_000L);

        Assert.assertEquals(Long.valueOf(5_000L), store.getArrivalTimeMillis("handle-one"));
    }

    @Test
    public void tracksDistinctHandlesIndependently() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.markNewActivity("handle-one", 1_000L);
        store.markNewActivity("handle-two", 2_000L);

        store.clearNewActivity("handle-one");

        Assert.assertFalse(store.hasNewActivity("handle-one"));
        Assert.assertTrue(store.hasNewActivity("handle-two"));
    }

    @Test
    public void formatsZeroElapsedAsSecondsAgo() {
        Assert.assertEquals("0s ago", SessionNewActivityStore.formatRelativeTime(0L));
    }

    @Test
    public void formatsNegativeElapsedAsZeroSecondsAgo() {
        Assert.assertEquals("0s ago", SessionNewActivityStore.formatRelativeTime(-5000L));
    }

    @Test
    public void formatsSubMinuteElapsedAsSecondsAgo() {
        Assert.assertEquals("30s ago", SessionNewActivityStore.formatRelativeTime(30_000L));
    }

    @Test
    public void formatsJustBelowOneMinuteAsSecondsAgo() {
        Assert.assertEquals("59s ago", SessionNewActivityStore.formatRelativeTime(59_999L));
    }

    @Test
    public void formatsExactlyOneMinuteAsMinutesAgo() {
        Assert.assertEquals("1m ago", SessionNewActivityStore.formatRelativeTime(60_000L));
    }

    @Test
    public void formatsSubHourElapsedAsMinutesAgo() {
        Assert.assertEquals("2m ago", SessionNewActivityStore.formatRelativeTime(150_000L));
    }

    @Test
    public void formatsJustBelowOneHourAsMinutesAgo() {
        Assert.assertEquals("59m ago", SessionNewActivityStore.formatRelativeTime(3_599_999L));
    }

    @Test
    public void formatsExactlyOneHourAsHoursAgo() {
        Assert.assertEquals("1h ago", SessionNewActivityStore.formatRelativeTime(3_600_000L));
    }

    @Test
    public void formatsMultipleHoursAsHoursAgo() {
        Assert.assertEquals("5h ago", SessionNewActivityStore.formatRelativeTime(5L * 3_600_000L));
    }
}
