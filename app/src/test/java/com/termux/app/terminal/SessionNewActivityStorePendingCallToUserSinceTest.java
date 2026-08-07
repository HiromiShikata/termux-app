package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStorePendingCallToUserSinceTest {

    @Test
    public void sessionWithNoCallAtAllHasNoPendingCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", 3_000L);

        Assert.assertNull(store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void unacknowledgedReasonAloneArmsThePendingCallAtItsRecordedTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 2_000L, "needs approval");

        Assert.assertEquals(Long.valueOf(2_000L), store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void statuslineCallNewerThanReplyAloneArmsThePendingCallAtTheCallTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 2_000L, 1_500L, 1_000L);

        Assert.assertEquals(Long.valueOf(2_000L), store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void statuslineReplyNewerThanCallLeavesNoPendingCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 1_000L, 1_500L, 2_000L);

        Assert.assertNull(store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void bothSignalsArmedReportsTheOlderOfTheTwo() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 5_000L, "needs approval");
        store.recordStatuslineTimes("worker", 2_000L, 1_500L, 1_000L);

        Assert.assertEquals(Long.valueOf(2_000L), store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void reconnectPurgeClearsTheReasonDerivedPendingCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 2_000L, "needs approval");

        store.purgeSessionKeepingTheCallAndReplyTimes("worker");

        Assert.assertNull(store.pendingCallToUserSinceTimeMillis("worker"));
    }

    @Test
    public void reconnectPurgePreservesTheStatuslineDerivedPendingCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("worker", 2_000L, 1_500L, 1_000L);

        store.purgeSessionKeepingTheCallAndReplyTimes("worker");

        Assert.assertEquals(Long.valueOf(2_000L), store.pendingCallToUserSinceTimeMillis("worker"));
    }
}
