package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class SessionNewActivityStoreTest {

    @Test
    public void recordsAndExposesLastBellTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastBellTimeMillis("session-one"));

        store.recordBell("session-one", 1_000L);

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastBellTimeMillis("session-one"));
    }

    @Test
    public void recordsAndExposesLastSeenTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));

        store.recordSeen("session-one", 2_000L);

        Assert.assertEquals(Long.valueOf(2_000L), store.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWithoutAnyBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertFalse(store.hasUnseenBell("session-one"));
    }

    @Test
    public void hasUnseenBellIsTrueWhenBellHasNeverBeenSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);

        Assert.assertTrue(store.hasUnseenBell("session-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWhenSeenAfterBell() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);
        store.recordSeen("session-one", 2_000L);

        Assert.assertFalse(store.hasUnseenBell("session-one"));
    }

    @Test
    public void hasUnseenBellIsFalseWhenSeenExactlyAtBellTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);
        store.recordSeen("session-one", 1_000L);

        Assert.assertFalse(store.hasUnseenBell("session-one"));
    }

    @Test
    public void hasUnseenBellIsTrueWhenBellArrivesAfterTheLastSeen() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("session-one", 1_000L);
        store.recordBell("session-one", 5_000L);

        Assert.assertTrue(store.hasUnseenBell("session-one"));
    }

    @Test
    public void purgeRemovesBothTimestamps() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);
        store.recordSeen("session-one", 2_000L);

        store.purgeSession("session-one");

        Assert.assertNull(store.getLastBellTimeMillis("session-one"));
        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void recordingBellAgainForSameNameUpdatesLastBellTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);
        store.recordBell("session-one", 5_000L);

        Assert.assertEquals(Long.valueOf(5_000L), store.getLastBellTimeMillis("session-one"));
    }

    @Test
    public void tracksDistinctNamesIndependently() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordBell("session-one", 1_000L);
        store.recordBell("session-two", 2_000L);
        store.recordSeen("session-one", 9_000L);

        Assert.assertFalse(store.hasUnseenBell("session-one"));
        Assert.assertTrue(store.hasUnseenBell("session-two"));
    }

    @Test
    public void unseenBellKeyedByNameSurvivesRestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordBell("my-session", 1_000L);
        Assert.assertTrue(beforeRestart.hasUnseenBell("my-session"));

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertTrue(afterRestart.hasUnseenBell("my-session"));
        Assert.assertEquals(Long.valueOf(1_000L), afterRestart.getLastBellTimeMillis("my-session"));
    }

    @Test
    public void reconstructingStoreFromSamePersistenceRestoresLastBellAndLastSeen() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("session-one", 1_000L);
        store.recordSeen("session-one", 2_000L);
        store.recordBell("session-two", 7_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastBellTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(2_000L), reconstructed.getLastSeenTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(7_000L), reconstructed.getLastBellTimeMillis("session-two"));
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
        store.recordBell("session-one", 1_000L);
        store.purgeSession("session-one");

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertNull(reconstructed.getLastBellTimeMillis("session-one"));
        Assert.assertNull(reconstructed.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void pruneToSessionNamesDropsUnknownNames() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("alive", 1_000L);
        store.recordBell("gone", 2_000L);
        store.recordSeen("gone", 3_000L);

        store.pruneToSessionNames(new HashSet<>(Collections.singletonList("alive")));

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastBellTimeMillis("alive"));
        Assert.assertNull(store.getLastBellTimeMillis("gone"));
        Assert.assertNull(store.getLastSeenTimeMillis("gone"));
    }

    @Test
    public void pruneToSessionNamesPersistsDroppedNamesAcrossReconstruction() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordBell("alive", 1_000L);
        store.recordBell("gone", 2_000L);

        store.pruneToSessionNames(new HashSet<>(Collections.singletonList("alive")));

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastBellTimeMillis("alive"));
        Assert.assertNull(reconstructed.getLastBellTimeMillis("gone"));
    }

    @Test
    public void pruneToSessionNamesRetainsRestoredNameThatStillExistsAfterRestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordBell("restored-session", 1_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);
        afterRestart.pruneToSessionNames(
            new HashSet<>(Collections.singletonList("restored-session")));

        Assert.assertTrue(afterRestart.hasUnseenBell("restored-session"));
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
