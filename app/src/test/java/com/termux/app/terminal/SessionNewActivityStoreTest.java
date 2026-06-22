package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class SessionNewActivityStoreTest {

    @Test
    public void recordsAndExposesLastOutputActivityTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastOutputActivityTimeMillis("session-one"));

        store.recordOutputActivity("session-one", 1_000L);

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastOutputActivityTimeMillis("session-one"));
    }

    @Test
    public void recordsAndExposesLastExplicitCallTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastExplicitCallTimeMillis("session-one"));

        store.recordExplicitCall("session-one", 1_000L);

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis("session-one"));
    }

    @Test
    public void recordsAndExposesLastSeenTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));

        store.recordSeen("session-one", 2_000L);

        Assert.assertEquals(Long.valueOf(2_000L), store.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void tierIsNoneWithoutAnySignal() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
        Assert.assertFalse(store.hasUnseenActivity("session-one"));
    }

    @Test
    public void outputActivityProducesYellowTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor("session-one"));
        Assert.assertTrue(store.hasUnseenActivity("session-one"));
    }

    @Test
    public void explicitCallProducesRedTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void explicitCallTakesPriorityOverOutputActivity() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 2_000L);
        store.recordExplicitCall("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void seenAfterBothSignalsClearsTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-one", 2_000L);
        store.recordSeen("session-one", 3_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
    }

    @Test
    public void outputSeenAtSameInstantClearsYellowForViewedSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordSeen("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
    }

    @Test
    public void viewedSessionWithNoNewOutputSinceLastSeenShowsNone() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordSeen("session-one", 1_000L);
        store.recordSeen("session-one", 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
    }

    @Test
    public void outputAfterSeenReappearsAsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordSeen("session-one", 1_000L);
        store.recordOutputActivity("session-one", 2_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor("session-one"));
    }

    @Test
    public void seenClearsRedButLeavesNewerOutputActivityAsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);
        store.recordSeen("session-one", 2_000L);
        store.recordOutputActivity("session-one", 3_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor("session-one"));
    }

    @Test
    public void tierClearedWhenSeenAtExactSignalTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);
        store.recordSeen("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
    }

    @Test
    public void signalAfterLastSeenReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("session-one", 1_000L);
        store.recordExplicitCall("session-one", 5_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void globalActiveTierIsRedWhenAnySessionRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-two", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.RED,
            store.globalActiveTier(new HashSet<>(java.util.Arrays.asList("session-one", "session-two"))));
    }

    @Test
    public void globalActiveTierIsYellowWhenAnyYellowAndNoRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW,
            store.globalActiveTier(new HashSet<>(java.util.Arrays.asList("session-one", "session-two"))));
    }

    @Test
    public void globalActiveTierIsNoneWhenNoSessionHasActivity() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(SessionNewActivityTier.NONE,
            store.globalActiveTier(new HashSet<>(java.util.Arrays.asList("session-one", "session-two"))));
    }

    @Test
    public void purgeRemovesAllTimestamps() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-one", 1_500L);
        store.recordSeen("session-one", 2_000L);

        store.purgeSession("session-one");

        Assert.assertNull(store.getLastOutputActivityTimeMillis("session-one"));
        Assert.assertNull(store.getLastExplicitCallTimeMillis("session-one"));
        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void tracksDistinctNamesIndependently() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);
        store.recordExplicitCall("session-two", 2_000L);
        store.recordSeen("session-one", 9_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-two"));
    }

    @Test
    public void twoTimestampModelKeyedByNameSurvivesRestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordOutputActivity("my-session", 1_000L);
        beforeRestart.recordExplicitCall("my-session", 2_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), afterRestart.getLastOutputActivityTimeMillis("my-session"));
        Assert.assertEquals(Long.valueOf(2_000L), afterRestart.getLastExplicitCallTimeMillis("my-session"));
        Assert.assertEquals(SessionNewActivityTier.RED, afterRestart.tierFor("my-session"));
    }

    @Test
    public void reconstructingStoreRestoresAllThreeTimestamps() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-one", 2_000L);
        store.recordSeen("session-one", 3_000L);
        store.recordOutputActivity("session-two", 7_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastOutputActivityTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(2_000L), reconstructed.getLastExplicitCallTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(3_000L), reconstructed.getLastSeenTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(7_000L), reconstructed.getLastOutputActivityTimeMillis("session-two"));
    }

    @Test
    public void reconstructedStoreReportsRedForBackgroundSession() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("background", 5_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.RED, reconstructed.tierFor("background"));
    }

    @Test
    public void reconstructedStoreReportsNoneForSessionThatWasSeen() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("seen", 5_000L);
        store.recordSeen("seen", 9_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.NONE, reconstructed.tierFor("seen"));
    }

    @Test
    public void purgePersistsRemovalAcrossReconstruction() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("session-one", 1_000L);
        store.purgeSession("session-one");

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertNull(reconstructed.getLastExplicitCallTimeMillis("session-one"));
        Assert.assertNull(reconstructed.getLastSeenTimeMillis("session-one"));
    }

    @Test
    public void pruneToSessionNamesDropsUnknownNames() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("alive", 1_000L);
        store.recordExplicitCall("gone", 2_000L);
        store.recordSeen("gone", 3_000L);

        store.pruneToSessionNames(new HashSet<>(Collections.singletonList("alive")));

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis("alive"));
        Assert.assertNull(store.getLastExplicitCallTimeMillis("gone"));
        Assert.assertNull(store.getLastSeenTimeMillis("gone"));
    }

    @Test
    public void pruneToSessionNamesPersistsDroppedNamesAcrossReconstruction() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("alive", 1_000L);
        store.recordExplicitCall("gone", 2_000L);

        store.pruneToSessionNames(new HashSet<>(Collections.singletonList("alive")));

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(1_000L), reconstructed.getLastExplicitCallTimeMillis("alive"));
        Assert.assertNull(reconstructed.getLastExplicitCallTimeMillis("gone"));
    }

    @Test
    public void pruneToSessionNamesRetainsRestoredNameThatStillExistsAfterRestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordExplicitCall("restored-session", 1_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);
        afterRestart.pruneToSessionNames(
            new HashSet<>(Collections.singletonList("restored-session")));

        Assert.assertEquals(SessionNewActivityTier.RED, afterRestart.tierFor("restored-session"));
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
    public void formatsExactlyOneMinuteAsMinutesAgo() {
        Assert.assertEquals("1m ago", SessionNewActivityStore.formatRelativeTime(60_000L));
    }

    @Test
    public void formatsExactlyOneHourAsHoursAgo() {
        Assert.assertEquals("1h ago", SessionNewActivityStore.formatRelativeTime(3_600_000L));
    }
}
