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

    @Test
    public void unrepliedCallStaysRedEvenWhenAStrayKeystrokeIsNewerThanTheCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long genuineReplyMillis = 1_000L;
        long callMillis = 2_000L;
        long strayKeystrokeMillis = 3_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, genuineReplyMillis);
        store.recordUserInput(SESSION, strayKeystrokeMillis);

        Assert.assertEquals(Long.valueOf(callMillis),
            store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void redClearsOnceAGenuineStatuslineReplyNewerThanTheCallIsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long genuineReplyMillis = 1_000L;
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, genuineReplyMillis);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        long newerGenuineReplyMillis = 3_000L;
        store.recordStatuslineTimes(SESSION, null, null, newerGenuineReplyMillis);

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void aRawKeystrokeAloneDoesNotClearRedWhenThereIsNoGenuineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, null);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        store.recordUserInput(SESSION, 5_000L);

        Assert.assertEquals(Long.valueOf(callMillis),
            store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }

    @Test
    public void aGenuineInAppReplyClearsRedImmediatelyWhileTheStatuslineReplyIsStillStale() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleStatuslineReplyMillis = 1_000L;
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, staleStatuslineReplyMillis);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        long genuineAppReplyMillis = 3_000L;
        store.recordGenuineAppReply(SESSION, genuineAppReplyMillis);

        Assert.assertNull("a genuine in-app reply submit newer than the call clears RED without "
            + "waiting for the laggy statusline reply token to catch up",
            store.statuslineCallPendingTimeMillis(SESSION));
        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals("the stale statusline reply token is left untouched by the in-app reply",
            Long.valueOf(staleStatuslineReplyMillis), store.getStatuslineReplyTimeMillis(SESSION));
    }

    @Test
    public void tagScanRunsForAFreshCallEvenWhenTheReplyAlreadyEqualsItOnFirstObservation() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);

        Assert.assertTrue(store.shouldScanCallToUserTag(SESSION));
    }

    @Test
    public void tagScanIsNotRepeatedForTheSameEqualCallAndReplyOnceItHasAlreadyBeenScanned() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);
        store.recordCallToUserTagScanPerformed(SESSION);

        Assert.assertFalse(store.shouldScanCallToUserTag(SESSION));
    }

    @Test
    public void anUnscannedEqualCallAndReplyStillReachesRedOnceTheScanRecordsItsReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 2_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);
        Assert.assertTrue(store.shouldScanCallToUserTag(SESSION));

        store.recordExplicitCall(SESSION, callMillis, "needs approval");
        store.recordCallToUserTagScanPerformed(SESSION);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }
}
