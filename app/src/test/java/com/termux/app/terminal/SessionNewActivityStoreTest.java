package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
        store.recordExplicitCall("session-one", 1_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void explicitCallTakesPriorityOverOutputActivity() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 2_000L);
        store.recordExplicitCall("session-one", 1_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void seenAfterBothSignalsLeavesRedBecauseTheUserHasNotReplied() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-one", 2_000L, "needs approval");
        store.recordSeen("session-one", 3_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void userInputAfterBothSignalsClearsTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-one", 2_000L);
        store.recordSeen("session-one", 3_000L);
        store.recordUserInput("session-one", 3_000L);

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
    public void userReplyClearsRedButLeavesNewerOutputActivityAsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);
        store.recordUserInput("session-one", 2_000L);
        store.recordOutputActivity("session-one", 3_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor("session-one"));
    }

    @Test
    public void redClearedWhenUserRepliesAtExactCallTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L);
        store.recordUserInput("session-one", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
    }

    @Test
    public void signalAfterLastSeenReappears() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen("session-one", 1_000L);
        store.recordExplicitCall("session-one", 5_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void globalActiveTierIsRedWhenAnySessionRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("session-one", 1_000L);
        store.recordExplicitCall("session-two", 1_000L, "needs approval");

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
        store.recordExplicitCall("session-one", 1_000L, "needs approval");
        store.recordExplicitCall("session-two", 2_000L, "needs approval");
        store.recordUserInput("session-one", 9_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-two"));
    }

    @Test
    public void twoTimestampModelKeyedByNameSurvivesRestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordOutputActivity("my-session", 1_000L);
        beforeRestart.recordExplicitCall("my-session", 2_000L, "needs approval");

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
        store.recordExplicitCall("background", 5_000L, "needs approval");

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.RED, reconstructed.tierFor("background"));
    }

    @Test
    public void reconstructedStoreReportsNoneForSessionTheUserHasRepliedTo() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("replied", 5_000L);
        store.recordUserInput("replied", 9_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.NONE, reconstructed.tierFor("replied"));
    }

    @Test
    public void reconstructedStoreStillReportsRedForBackgroundSessionThatWasOnlySeen() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordExplicitCall("seen", 5_000L, "needs approval");
        store.recordSeen("seen", 9_000L);

        SessionNewActivityStore reconstructed = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.RED, reconstructed.tierFor("seen"));
    }

    @Test
    public void recordsAndExposesLastUserInputTimeByName() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertNull(store.getLastUserInputTimeMillis("session-one"));

        store.recordUserInput("session-one", 4_000L);

        Assert.assertEquals(Long.valueOf(4_000L), store.getLastUserInputTimeMillis("session-one"));
    }

    @Test
    public void newCallAfterAPriorReplyMakesRedReappear() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L, "first approval");
        store.recordUserInput("session-one", 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("session-one"));

        store.recordExplicitCall("session-one", 5_000L, "second approval");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("session-one"));
    }

    @Test
    public void lastUserInputTimeSurvivesPersistenceReload() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordExplicitCall("worker", 1_000L);
        beforeRestart.recordUserInput("worker", 2_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(2_000L), afterRestart.getLastUserInputTimeMillis("worker"));
        Assert.assertEquals(SessionNewActivityTier.NONE, afterRestart.tierFor("worker"));
    }

    @Test
    public void purgeRemovesUserInputTimestamp() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput("session-one", 1_000L);

        store.purgeSession("session-one");

        Assert.assertNull(store.getLastUserInputTimeMillis("session-one"));
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
        beforeRestart.recordExplicitCall("restored-session", 1_000L, "needs approval");

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

    @Test
    public void lastOutputActivityAgeLabelReflectsRecordedOutputActivityInEnglish() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        Assert.assertEquals("5s ago", store.lastOutputActivityAgeLabel("worker", 6_000L));
    }

    @Test
    public void lastUserInputAgeLabelReflectsRecordedUserInputInEnglish() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput("worker", 1_000L);

        Assert.assertEquals("5s ago", store.lastUserInputAgeLabel("worker", 6_000L));
    }

    @Test
    public void lastUserInputAgeLabelIsNullWhenUserInputUnknown() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        Assert.assertNull(store.lastUserInputAgeLabel("worker", 6_000L));
    }

    @Test
    public void lastOutputActivityAgeLabelIsNullWhenOutputActivityUnknown() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L);

        Assert.assertNull(store.lastOutputActivityAgeLabel("worker", 6_000L));
    }

    @Test
    public void lastOutputActivityAgeLabelIsPresentEvenWithoutAnyPendingTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);
        store.recordSeen("worker", 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
        Assert.assertEquals("0s ago", store.lastOutputActivityAgeLabel("worker", 1_500L));
    }

    @Test
    public void recordExplicitCallWithReasonStoresReasonAndRaisesRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "deploy failed");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertEquals("deploy failed", store.getLastExplicitCallReason("worker"));
    }

    @Test
    public void recordExplicitCallWithoutReasonDoesNotRaiseRedSoTheIndicatorNeverShowsAnEmptyScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
        Assert.assertEquals("", store.getLastExplicitCallReason("worker"));
    }

    @Test
    public void getLastExplicitCallReasonIsEmptyForUnknownSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals("", store.getLastExplicitCallReason("never-called"));
    }

    @Test
    public void recordExplicitCallWithReasonOverwritesPreviousReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordExplicitCall("worker", 2_000L, "second reason");

        Assert.assertEquals("second reason", store.getLastExplicitCallReason("worker"));
    }

    @Test
    public void purgeSessionClearsExplicitCallReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "deploy failed");
        store.purgeSession("worker");

        Assert.assertEquals("", store.getLastExplicitCallReason("worker"));
    }

    @Test
    public void explicitCallReasonSurvivesPersistenceReload() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordExplicitCall("worker", 1_000L, "needs approval");

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals("needs approval", afterRestart.getLastExplicitCallReason("worker"));
    }

    @Test
    public void unacknowledgedCallReasonsAreEmptyForUnknownSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertTrue(store.getUnacknowledgedCallReasons("never-called").isEmpty());
    }

    @Test
    public void multipleExplicitCallsAccumulateAllUnacknowledgedReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordExplicitCall("worker", 2_000L, "second reason");
        store.recordExplicitCall("worker", 3_000L, "third reason");

        Assert.assertEquals(
            java.util.Arrays.asList("first reason", "second reason", "third reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void reRecordingAnAlreadyPresentReasonDoesNotAppendADuplicate() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "deploy failed");
        store.recordExplicitCall("worker", 2_000L, "deploy failed");
        store.recordExplicitCall("worker", 3_000L, "deploy failed");

        Assert.assertEquals(
            Collections.singletonList("deploy failed"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void reScanningTheSameReasonsManyTimesKeepsEachReasonExactlyOnce() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        for (int scan = 0; scan < 20; scan++) {
            store.recordExplicitCall("worker", 1_000L + scan, "first reason");
            store.recordExplicitCall("worker", 2_000L + scan, "second reason");
        }

        Assert.assertEquals(
            java.util.Arrays.asList("first reason", "second reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void distinctReasonsStillAccumulateWhenInterleavedWithDuplicateReRecords() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordExplicitCall("worker", 2_000L, "first reason");
        store.recordExplicitCall("worker", 3_000L, "second reason");
        store.recordExplicitCall("worker", 4_000L, "first reason");
        store.recordExplicitCall("worker", 5_000L, "second reason");

        Assert.assertEquals(
            java.util.Arrays.asList("first reason", "second reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void explicitCallWithEmptyReasonDoesNotAddToUnacknowledgedReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordExplicitCall("worker", 2_000L, "");
        store.recordExplicitCall("worker", 3_000L, "   ");

        Assert.assertEquals(
            Collections.singletonList("first reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void userReplyClearsUnacknowledgedCallReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordExplicitCall("worker", 2_000L, "second reason");

        store.recordUserInput("worker", 3_000L);

        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void newCallAfterReplyStartsAFreshUnacknowledgedReasonsList() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordUserInput("worker", 2_000L);

        store.recordExplicitCall("worker", 3_000L, "third reason");

        Assert.assertEquals(
            Collections.singletonList("third reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void purgeSessionClearsUnacknowledgedCallReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");

        store.purgeSession("worker");

        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void pruneToSessionNamesDropsUnacknowledgedReasonsForUnknownNames() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("alive", 1_000L, "alive reason");
        store.recordExplicitCall("gone", 2_000L, "gone reason");

        store.pruneToSessionNames(new HashSet<>(Collections.singletonList("alive")));

        Assert.assertEquals(
            Collections.singletonList("alive reason"),
            store.getUnacknowledgedCallReasons("alive"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("gone").isEmpty());
    }

    @Test
    public void unacknowledgedCallReasonsSurvivePersistenceReload() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordExplicitCall("worker", 1_000L, "first reason");
        beforeRestart.recordExplicitCall("worker", 2_000L, "second reason");

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals(
            java.util.Arrays.asList("first reason", "second reason"),
            afterRestart.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void getUnacknowledgedCallReasonsReturnsImmutableCopy() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");

        try {
            store.getUnacknowledgedCallReasons("worker").add("mutated");
            Assert.fail("Expected returned list to be immutable");
        } catch (UnsupportedOperationException expected) {
            // The store must not expose a mutable view of its internal reasons list.
        }

        Assert.assertEquals(
            Collections.singletonList("first reason"),
            store.getUnacknowledgedCallReasons("worker"));
    }

    @Test
    public void reDetectingAnAlreadyRecordedCallKeepsItsFirstDetectionTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");

        store.recordExplicitCall("worker", 5_000L, "needs approval");
        store.recordExplicitCall("worker", 9_000L, "needs approval");

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis("worker"));
    }

    @Test
    public void reDetectingAnAlreadyRecordedCallDoesNotReArmTheRedTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        store.recordSeen("worker", 2_000L);
        store.recordUserInput("worker", 3_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));

        store.recordExplicitCall("worker", 9_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void reDetectingAnAlreadyClearedCallAfterReplyDoesNotRefreshItsTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        store.recordUserInput("worker", 3_000L);

        store.recordExplicitCall("worker", 9_000L, "needs approval");

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis("worker"));
    }

    @Test
    public void aGenuinelyNewReasonStillFiresAfterAPriorReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "first reason");
        store.recordUserInput("worker", 2_000L);

        store.recordExplicitCall("worker", 5_000L, "second reason");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertEquals(Long.valueOf(5_000L), store.getLastExplicitCallTimeMillis("worker"));
    }

    @Test
    public void reDetectingAReflowedReasonDoesNotReArmTheRedTierAfterReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "please approve the deploy to production now");
        store.recordUserInput("worker", 3_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));

        // The same reason re-rendered after a column resize wraps across a different line boundary.
        store.recordExplicitCall("worker", 9_000L, "please approve the deploy\nto production now");

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
    }

    @Test
    public void reDetectingAReflowedReasonKeepsItsFirstDetectionTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs    review of   the diff");

        // Reflow collapses or shifts interior whitespace but the call is the same.
        store.recordExplicitCall("worker", 9_000L, "needs review of the diff");

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis("worker"));
    }

    @Test
    public void clearedCallStaysClearedAcrossPersistenceReloadWhenReDetected() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeRestart = new SessionNewActivityStore(persistence);
        beforeRestart.recordExplicitCall("worker", 1_000L, "needs approval");
        beforeRestart.recordUserInput("worker", 2_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);
        afterRestart.recordExplicitCall("worker", 9_000L, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.NONE, afterRestart.tierFor("worker"));
        Assert.assertEquals(Long.valueOf(1_000L), afterRestart.getLastExplicitCallTimeMillis("worker"));
    }

    @Test
    public void statuslineTimesSetCallOutAndReplyDirectly() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordStatuslineTimes("worker", 3_000L, 5_000L, 4_000L);

        Assert.assertEquals(Long.valueOf(3_000L), store.getLastExplicitCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(5_000L), store.getLastOutputActivityTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(4_000L), store.getLastUserInputTimeMillis("worker"));
    }

    @Test
    public void statuslineCallTokenAloneDoesNotDriveRedTierBecauseItCarriesNoScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordStatuslineTimes("worker", 9_000L, 9_000L, 2_000L);

        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertFalse(store.hasPendingExplicitCall("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void statuslineReplyNewerThanCallAcknowledgesPendingCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");

        store.recordStatuslineTimes("worker", 1_000L, 1_000L, 5_000L);

        Assert.assertFalse(store.hasPendingExplicitCall("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void statuslineTimesIgnoreNullComponents() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);

        store.recordStatuslineTimes("worker", null, 7_000L, null);

        Assert.assertNull(store.getLastExplicitCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(7_000L), store.getLastOutputActivityTimeMillis("worker"));
        Assert.assertNull(store.getLastUserInputTimeMillis("worker"));
    }

    @Test
    public void statuslineDisplayGettersExposeOnlyStatuslineSourcedCallOutAndReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordStatuslineTimes("worker", 3_000L, 5_000L, 4_000L);

        Assert.assertEquals(Long.valueOf(3_000L), store.getStatuslineCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(5_000L), store.getStatuslineOutTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(4_000L), store.getStatuslineReplyTimeMillis("worker"));
    }

    @Test
    public void statuslineDisplayGettersStayAbsentForGenuineActivityRecordersSoNoFakeTimeIsSubstituted() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 1_000L);
        store.recordExplicitCall("worker", 2_000L, "needs approval");
        store.recordUserInput("worker", 3_000L);
        store.recordSeen("worker", 4_000L);

        Assert.assertNull(store.getStatuslineCallTimeMillis("worker"));
        Assert.assertNull(store.getStatuslineOutTimeMillis("worker"));
        Assert.assertNull(store.getStatuslineReplyTimeMillis("worker"));
    }

    @Test
    public void statuslineDisplayGettersIgnoreNullComponentsSoAbsentTokensStayUnknown() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordStatuslineTimes("worker", null, 7_000L, null);

        Assert.assertNull(store.getStatuslineCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(7_000L), store.getStatuslineOutTimeMillis("worker"));
        Assert.assertNull(store.getStatuslineReplyTimeMillis("worker"));
    }

    @Test
    public void statuslineDisplayTimesSurviveRestartThroughPersistence() {
        InMemorySessionNewActivityPersistence persistence =
            new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 3_000L, 5_000L, 4_000L);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(3_000L), afterRestart.getStatuslineCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(5_000L), afterRestart.getStatuslineOutTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(4_000L), afterRestart.getStatuslineReplyTimeMillis("worker"));
    }

    @Test
    public void purgeSessionClearsStatuslineDisplayTimes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 3_000L, 5_000L, 4_000L);

        store.purgeSession("worker");

        Assert.assertNull(store.getStatuslineCallTimeMillis("worker"));
        Assert.assertNull(store.getStatuslineOutTimeMillis("worker"));
        Assert.assertNull(store.getStatuslineReplyTimeMillis("worker"));
    }

    private static void assertTierAndSceneAgree(SessionNewActivityStore store, String sessionName) {
        boolean isRed = store.tierFor(sessionName) == SessionNewActivityTier.RED;
        boolean hasReasons = !store.getUnacknowledgedCallReasons(sessionName).isEmpty();
        Assert.assertEquals("the RED call-to-user tier and the unacknowledged-reasons scene must "
            + "always agree: there must be no RED-with-empty-scene state and no scene-with-no-pending-"
            + "call state for session " + sessionName, isRed, hasReasons);
    }

    private static String mostRecentReason(SessionNewActivityStore store, String sessionName) {
        java.util.List<String> reasons = store.getUnacknowledgedCallReasons(sessionName);
        return reasons.isEmpty() ? null : reasons.get(reasons.size() - 1);
    }

    @Test
    public void ownerInputAcknowledgesTheCallClearingBothTheIndicatorAndTheSceneAndBumpsReplyTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertFalse(store.getUnacknowledgedCallReasons("worker").isEmpty());

        store.recordUserInput("worker", 5_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("worker"));
        Assert.assertFalse(store.hasPendingExplicitCall("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
        Assert.assertEquals(Long.valueOf(5_000L), store.getLastUserInputTimeMillis("worker"));
    }

    @Test
    public void ownerInputAcknowledgesImmediatelyEvenForACallRecordedMoreThanADayEarlier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callTimeMillis = 1_000L;
        long replyTimeMillis = callTimeMillis + 2L * 24L * 60L * 60L * 1000L;
        store.recordExplicitCall("worker", callTimeMillis, "needs approval");
        Assert.assertTrue(store.hasPendingExplicitCall("worker"));

        store.recordUserInput("worker", replyTimeMillis);

        Assert.assertFalse(store.hasPendingExplicitCall("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
        assertTierAndSceneAgree(store, "worker");
    }

    @Test
    public void aGenuinelyPendingCallShowsTheScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");

        PendingCallToUserFooterDecision decision = PendingCallToUserFooterDecision.resolve(
            store.tierFor("worker"), mostRecentReason(store, "worker"));

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("worker"));
        Assert.assertTrue(decision.isVisible());
        Assert.assertEquals("needs approval", decision.getReportText());
        assertTierAndSceneAgree(store, "worker");
    }

    @Test
    public void aStatuslineCallTokenArrivingAfterAReplyDoesNotReArmRedWithAnEmptyScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        store.recordUserInput("worker", 2_000L);
        assertTierAndSceneAgree(store, "worker");

        store.recordStatuslineTimes("worker", 9_000L, 9_000L, 2_000L);

        Assert.assertFalse(store.hasPendingExplicitCall("worker"));
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
        assertTierAndSceneAgree(store, "worker");
    }

    @Test
    public void tierAndSceneStayConsistentAcrossRecordStatuslineInputPurgeAndReload() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);

        store.recordExplicitCall("worker", 1_000L, "needs approval");
        assertTierAndSceneAgree(store, "worker");

        store.recordStatuslineTimes("worker", 1_000L, 1_000L, 500L);
        assertTierAndSceneAgree(store, "worker");

        store.recordOutputActivity("worker", 1_200L);
        assertTierAndSceneAgree(store, "worker");

        store.recordSeen("worker", 1_300L);
        assertTierAndSceneAgree(store, "worker");

        store.recordUserInput("worker", 2_000L);
        assertTierAndSceneAgree(store, "worker");

        store.recordStatuslineTimes("worker", 9_000L, 9_000L, 2_000L);
        assertTierAndSceneAgree(store, "worker");

        SessionNewActivityStore reloaded = new SessionNewActivityStore(persistence);
        assertTierAndSceneAgree(reloaded, "worker");

        reloaded.recordExplicitCall("worker", 10_000L, "second approval");
        assertTierAndSceneAgree(reloaded, "worker");

        reloaded.purgeSession("worker");
        assertTierAndSceneAgree(reloaded, "worker");
    }

    @Test
    public void aReasonBearingCallThatSurvivesReloadKeepsTheTierAndSceneInAgreement() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeReload = new SessionNewActivityStore(persistence);
        beforeReload.recordExplicitCall("worker", 1_000L, "needs approval");

        SessionNewActivityStore afterReload = new SessionNewActivityStore(persistence);

        Assert.assertEquals(SessionNewActivityTier.RED, afterReload.tierFor("worker"));
        Assert.assertFalse(afterReload.getUnacknowledgedCallReasons("worker").isEmpty());
        assertTierAndSceneAgree(afterReload, "worker");
    }

    @Test
    public void recordStatuslineTimesWithNewValuesPersists() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        int saveCountBefore = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L);

        Assert.assertEquals(saveCountBefore + 1, persistence.getSaveCount());
        Assert.assertEquals(Long.valueOf(1_000L), store.getStatuslineCallTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(2_000L), store.getStatuslineOutTimeMillis("worker"));
        Assert.assertEquals(Long.valueOf(3_000L), store.getStatuslineReplyTimeMillis("worker"));
    }

    @Test
    public void recordStatuslineTimesWithUnchangedValuesDoesNotPersistAgain() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L);
        int saveCountAfterFirstRecord = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L);

        Assert.assertEquals(saveCountAfterFirstRecord, persistence.getSaveCount());
    }

    @Test
    public void recordStatuslineTimesPersistsAgainWhenOnlyOutChanges() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L);
        int saveCountAfterFirstRecord = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", 1_000L, 9_000L, 3_000L);

        Assert.assertEquals(saveCountAfterFirstRecord + 1, persistence.getSaveCount());
        Assert.assertEquals(Long.valueOf(9_000L), store.getStatuslineOutTimeMillis("worker"));
    }

    @Test
    public void recordStatuslineTimesWithAbsentTokensAfterStoredValuesDoesNotPersistAgain() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L);
        int saveCountAfterFirstRecord = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", null, null, null);

        Assert.assertEquals(saveCountAfterFirstRecord, persistence.getSaveCount());
    }

    @Test
    public void unchangedReplyStillAcknowledgesAPendingCallReasonRecordedAfterTheReply() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", null, null, 5_000L);
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        Assert.assertFalse(store.getUnacknowledgedCallReasons("worker").isEmpty());
        int saveCountBeforeReplayedReply = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", null, null, 5_000L);

        Assert.assertEquals(saveCountBeforeReplayedReply + 1, persistence.getSaveCount());
        Assert.assertTrue(store.getUnacknowledgedCallReasons("worker").isEmpty());
    }

    @Test
    public void subagentCountDefaultsToZeroForAnUnknownSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(0, store.getSubagentCount("worker"));
    }

    @Test
    public void recordStatuslineTimesStoresTheSubagentCount() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L, 5);

        Assert.assertEquals(5, store.getSubagentCount("worker"));
    }

    @Test
    public void recordStatuslineTimesPersistsAgainWhenOnlyTheSubagentCountChanges() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L, 1);
        int saveCountAfterFirstRecord = persistence.getSaveCount();

        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L, 4);

        Assert.assertEquals(saveCountAfterFirstRecord + 1, persistence.getSaveCount());
        Assert.assertEquals(4, store.getSubagentCount("worker"));
    }

    @Test
    public void subagentCountSurvivesAStoreReloadFromPersistence() {
        SaveCountingPersistence persistence = new SaveCountingPersistence();
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes("worker", 1_000L, 2_000L, 3_000L, 6);

        SessionNewActivityStore reloadedStore = new SessionNewActivityStore(persistence);

        Assert.assertEquals(6, reloadedStore.getSubagentCount("worker"));
    }

    private static final class SaveCountingPersistence implements SessionNewActivityPersistence {

        private List<SessionNewActivityState> mStates = new ArrayList<>();
        private int mSaveCount;

        @NonNull
        @Override
        public List<SessionNewActivityState> load() {
            return new ArrayList<>(mStates);
        }

        @Override
        public void save(@NonNull List<SessionNewActivityState> states) {
            mStates = new ArrayList<>(states);
            mSaveCount++;
        }

        int getSaveCount() {
            return mSaveCount;
        }
    }
}
