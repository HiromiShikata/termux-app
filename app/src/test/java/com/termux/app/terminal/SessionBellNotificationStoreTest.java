package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionBellNotificationStoreTest {

    @Test
    public void formatsZeroElapsedAsSecondsAgo() {
        Assert.assertEquals("0s ago", SessionBellNotificationStore.formatRelativeTime(0L));
    }

    @Test
    public void formatsNegativeElapsedAsZeroSecondsAgo() {
        Assert.assertEquals("0s ago", SessionBellNotificationStore.formatRelativeTime(-5000L));
    }

    @Test
    public void formatsSubMinuteElapsedAsSecondsAgo() {
        Assert.assertEquals("30s ago", SessionBellNotificationStore.formatRelativeTime(30_000L));
    }

    @Test
    public void formatsJustBelowOneMinuteAsSecondsAgo() {
        Assert.assertEquals("59s ago", SessionBellNotificationStore.formatRelativeTime(59_999L));
    }

    @Test
    public void formatsExactlyOneMinuteAsMinutesAgo() {
        Assert.assertEquals("1m ago", SessionBellNotificationStore.formatRelativeTime(60_000L));
    }

    @Test
    public void formatsSubHourElapsedAsMinutesAgo() {
        Assert.assertEquals("2m ago", SessionBellNotificationStore.formatRelativeTime(150_000L));
    }

    @Test
    public void formatsJustBelowOneHourAsMinutesAgo() {
        Assert.assertEquals("59m ago", SessionBellNotificationStore.formatRelativeTime(3_599_999L));
    }

    @Test
    public void formatsExactlyOneHourAsHoursAgo() {
        Assert.assertEquals("1h ago", SessionBellNotificationStore.formatRelativeTime(3_600_000L));
    }

    @Test
    public void formatsMultipleHoursAsHoursAgo() {
        Assert.assertEquals("5h ago", SessionBellNotificationStore.formatRelativeTime(5L * 3_600_000L));
    }

    @Test
    public void recordsAndClearsPendingNotificationByHandle() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        Assert.assertFalse(store.hasPendingNotification("handle-one"));

        store.recordBell("handle-one", 1_000L);
        Assert.assertTrue(store.hasPendingNotification("handle-one"));
        Assert.assertEquals(Long.valueOf(1_000L), store.getBellArrivalTimeMillis("handle-one"));

        store.clearBell("handle-one");
        Assert.assertFalse(store.hasPendingNotification("handle-one"));
        Assert.assertNull(store.getBellArrivalTimeMillis("handle-one"));
    }

    @Test
    public void recordingAgainForSameHandleUpdatesArrivalTime() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        store.recordBell("handle-one", 1_000L);
        store.recordBell("handle-one", 5_000L);

        Assert.assertEquals(Long.valueOf(5_000L), store.getBellArrivalTimeMillis("handle-one"));
    }

    @Test
    public void tracksNotificationsForDistinctHandlesIndependently() {
        SessionBellNotificationStore store = new SessionBellNotificationStore();
        store.recordBell("handle-one", 1_000L);
        store.recordBell("handle-two", 2_000L);

        store.clearBell("handle-one");

        Assert.assertFalse(store.hasPendingNotification("handle-one"));
        Assert.assertTrue(store.hasPendingNotification("handle-two"));
    }
}
