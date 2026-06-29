package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStorePreserveStatuslineOnReconnectTest {

    @Test
    public void preservingPurgeKeepsTheDisplayedCallOutAndReplyTimes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 1);

        store.purgeSessionPreservingStatuslineTimes("session-one");

        Assert.assertEquals(Long.valueOf(1_000L), store.getStatuslineCallTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(2_000L), store.getStatuslineOutTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(3_000L), store.getStatuslineReplyTimeMillis("session-one"));
        Assert.assertEquals(1, store.getSubagentCount("session-one"));
    }

    @Test
    public void preservingPurgeKeepsTheDotTierSourceSoTheRowDoesNotJumpToMoreThanOneDay() {
        long now = 5_000L;
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 4_900L, 1_000L, 0);

        store.purgeSessionPreservingStatuslineTimes("session-one");

        Long dotTierSource = store.outActivityTimeMillisForDotTier("session-one");
        Assert.assertEquals(Long.valueOf(4_900L), dotTierSource);
        Assert.assertNotEquals(SessionNewActivityStore.MORE_THAN_ONE_DAY_LABEL,
            SessionNewActivityStore.formatRelativeAge(dotTierSource, now));
    }

    @Test
    public void preservingPurgeClearsTheSeenAndUserInputBookkeeping() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 0);
        store.recordSeen("session-one", 2_500L);
        store.recordUserInput("session-one", 2_600L);

        store.purgeSessionPreservingStatuslineTimes("session-one");

        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));
        Assert.assertNull(store.getLastUserInputTimeMillis("session-one"));
    }

    @Test
    public void preservingPurgeClearsThePendingCallToUserReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L, "please review");
        Assert.assertFalse(store.getUnacknowledgedCallReasons("session-one").isEmpty());

        store.purgeSessionPreservingStatuslineTimes("session-one");

        Assert.assertTrue(store.getUnacknowledgedCallReasons("session-one").isEmpty());
        Assert.assertNull(store.currentPendingCallToUserReason("session-one"));
    }

    @Test
    public void fullPurgeStillBlanksTheStatuslineTimes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 0);

        store.purgeSession("session-one");

        Assert.assertNull(store.getStatuslineCallTimeMillis("session-one"));
        Assert.assertNull(store.getStatuslineOutTimeMillis("session-one"));
        Assert.assertNull(store.getStatuslineReplyTimeMillis("session-one"));
    }

    @Test
    public void hasStoredStatuslineDataReflectsWhetherAnyStatuslineTokenWasRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        Assert.assertFalse(store.hasStoredStatuslineData("session-one"));

        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 0);
        Assert.assertTrue(store.hasStoredStatuslineData("session-one"));

        store.purgeSession("session-one");
        Assert.assertFalse(store.hasStoredStatuslineData("session-one"));
    }
}
