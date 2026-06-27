package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStatuslinePendingRedTest {

    private static final String SESSION = "worker-session";

    @Test
    public void statuslineCallNewerThanReplyIsRedEvenWithoutAnyTagScan() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = 1_000L;
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, replyMillis);

        Assert.assertEquals(Long.valueOf(callMillis),
            store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void statuslineCallWithNoReplyYetIsRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, null);

        Assert.assertEquals(Long.valueOf(callMillis),
            store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void statuslineReplyNewerThanCallIsNotRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000L;
        long replyMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, replyMillis, replyMillis);

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void statuslineReplyEqualToCallIsNotRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void nonStatuslineSessionStillGoesRedThroughTheTagFallback() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordExplicitCall(SESSION, 1_000L, "needs approval");

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void statuslinePendingArmsRedEvenWhenTheTagScanMissedTheTag() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = 1_000L;
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, replyMillis);

        Assert.assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void tagScanRunsOnlyWhenAStatuslinePendingCallExists() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long replyMillis = 1_000L;
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, replyMillis);

        Assert.assertTrue(store.shouldScanCallToUserTag(SESSION));
    }

    @Test
    public void tagScanIsSkippedWhenTheStatuslineReplyHasCaughtUpToTheCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000L;
        long replyMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, replyMillis, replyMillis);

        Assert.assertFalse(store.shouldScanCallToUserTag(SESSION));
    }

    @Test
    public void tagScanRunsForANonStatuslineSessionAsTheFallback() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertTrue(store.shouldScanCallToUserTag(SESSION));
    }

    @Test
    public void tagScanRunsForASessionThatOnlyEverEmittedRawOutputWithoutAStatusline() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordOutputActivity(SESSION, 1_000L);

        Assert.assertTrue(store.shouldScanCallToUserTag(SESSION));
    }
}
