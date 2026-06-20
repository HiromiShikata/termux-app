package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class SessionNewActivityStoreTest {

    @Test
    public void recordsAndExposesLastBellTimeByHandle() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastBellTimeMillis("handle-one"));

        store.recordBell("handle-one", 1_000L);

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastBellTimeMillis("handle-one"));
    }

    @Test
    public void recordsAndExposesLastSeenTimeByHandle() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastSeenTimeMillis("handle-one"));

        store.recordSeen("handle-one", 2_000L);

        Assert.assertEquals(Long.valueOf(2_000L), store.getLastSeenTimeMillis("handle-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWithoutAnyBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertFalse(store.hasUnseenBell("handle-one"));
    }

    @Test
    public void hasUnseenBellIsTrueWhenBellHasNeverBeenSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);

        Assert.assertTrue(store.hasUnseenBell("handle-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWhenSeenAfterBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);
        store.recordSeen("handle-one", 2_000L);

        Assert.assertFalse(store.hasUnseenBell("handle-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWhenSeenExactlyAtBellTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);
        store.recordSeen("handle-one", 1_000L);

        Assert.assertFalse(store.hasUnseenBell("handle-one"));
    }

    @Test
    public void hasUnseenBellIsTrueWhenBellArrivesAfterTheLastSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("handle-one", 1_000L);
        store.recordBell("handle-one", 5_000L);

        Assert.assertTrue(store.hasUnseenBell("handle-one"));
    }

    @Test
    public void purgeRemovesBothTimestamps() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);
        store.recordSeen("handle-one", 2_000L);

        store.purgeSession("handle-one");

        Assert.assertNull(store.getLastBellTimeMillis("handle-one"));
        Assert.assertNull(store.getLastSeenTimeMillis("handle-one"));
    }

    @Test
    public void recordingBellAgainForSameHandleUpdatesLastBellTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);
        store.recordBell("handle-one", 5_000L);

        Assert.assertEquals(Long.valueOf(5_000L), store.getLastBellTimeMillis("handle-one"));
    }

    @Test
    public void tracksDistinctHandlesIndependently() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("handle-one", 1_000L);
        store.recordBell("handle-two", 2_000L);
        store.recordSeen("handle-one", 9_000L);

        Assert.assertFalse(store.hasUnseenBell("handle-one"));
        Assert.assertTrue(store.hasUnseenBell("handle-two"));
    }

    @Test
    public void reconstructingStoreFromSamePersistenceRestoresLastBellAndLastSeen() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("handle-one", 1_000L);
        store.recordSeen("handle-one", 2_000L);
        store.recordBell("handle-two", 7_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastBellTimeMillis("handle-one"));
        Assert.assertEquals(Long.valueOf(2_000L), reconstructed.getLastSeenTimeMillis("handle-one"));
        Assert.assertEquals(Long.valueOf(7_000L), reconstructed.getLastBellTimeMillis("handle-two"));
    }

    @Test
    public void reconstructedStoreReportsUnseenBellForBackgroundSession() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("background", 5_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertTrue(reconstructed.hasUnseenBell("background"));
    }

    @Test
    public void reconstructedStoreReportsNoBellForSessionThatWasSeen() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("seen", 5_000L);
        store.recordSeen("seen", 9_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertFalse(reconstructed.hasUnseenBell("seen"));
    }

    @Test
    public void purgePersistsRemovalAcrossReconstruction() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("handle-one", 1_000L);
        store.purgeSession("handle-one");

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertNull(reconstructed.getLastBellTimeMillis("handle-one"));
        Assert.assertNull(reconstructed.getLastSeenTimeMillis("handle-one"));
    }

    @Test
    public void pruneToHandlesDropsUnknownHandles() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("alive", 1_000L);
        store.recordBell("gone", 2_000L);
        store.recordSeen("gone", 3_000L);

        store.pruneToHandles(new HashSet<>(Collections.singletonList("alive")));

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastBellTimeMillis("alive"));
        Assert.assertNull(store.getLastBellTimeMillis("gone"));
        Assert.assertNull(store.getLastSeenTimeMillis("gone"));
    }

    @Test
    public void pruneToHandlesPersistsDroppedHandlesAcrossReconstruction() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("alive", 1_000L);
        store.recordBell("gone", 2_000L);

        store.pruneToHandles(new HashSet<>(Collections.singletonList("alive")));

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastBellTimeMillis("alive"));
        Assert.assertNull(reconstructed.getLastBellTimeMillis("gone"));
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
