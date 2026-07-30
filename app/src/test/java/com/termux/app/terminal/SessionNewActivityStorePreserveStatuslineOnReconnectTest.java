package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStorePreserveStatuslineOnReconnectTest {

    @Test
    public void preservingPurgeKeepsTheDisplayedCallAndReplyTimesAndDropsTheOutTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 1);

        store.purgeSessionKeepingTheCallAndReplyTimes("session-one");

        Assert.assertEquals(Long.valueOf(1_000L), store.getStatuslineCallTimeMillis("session-one"));
        Assert.assertNull("the out time is what the hung judgement reads and the replacement has "
                + "produced no output yet, so an in-place reconnect must not hand it over",
            store.getStatuslineOutTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(3_000L), store.getStatuslineReplyTimeMillis("session-one"));
        Assert.assertEquals(1, store.getSubagentCount("session-one"));
    }

    @Test
    public void preservingPurgeDropsTheOutDotTierAndKeepsTheReplyDotTierSourceSoTheRowDoesNotJumpToMoreThanOneDay() {
        long now = 5_000L;
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 4_900L, 1_000L, 0);

        store.purgeSessionKeepingTheCallAndReplyTimes("session-one");

        Assert.assertNull("the replacement has produced no output yet, so the dot tier must not age "
                + "off the output time of the session it replaced",
            store.outActivityTimeMillisForDotTier("session-one"));
        Long dotTierSource = store.replyActivityTimeMillisForDotTier("session-one");
        Assert.assertEquals(Long.valueOf(1_000L), dotTierSource);
        Assert.assertNotEquals(SessionNewActivityStore.MORE_THAN_ONE_DAY_LABEL,
            SessionNewActivityStore.formatRelativeAge(dotTierSource, now));
    }

    @Test
    public void preservingPurgeClearsTheSeenBookkeepingButKeepsTheOwnerInputOverwrite() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", 1_000L, 2_000L, 3_000L, 0);
        store.recordSeen("session-one", 2_500L);
        store.recordUserInput("session-one", 2_600L);

        store.purgeSessionKeepingTheCallAndReplyTimes("session-one");

        Assert.assertNull(store.getLastSeenTimeMillis("session-one"));
        Assert.assertEquals(Long.valueOf(2_600L), store.getLastUserInputTimeMillis("session-one"));
    }

    @Test
    public void preservingPurgeKeepsTheOnSendReplyOverwriteSoItDoesNotRevertToTheStaleStatuslineReply() {
        long staleStatuslineReply = 1_000L;
        long ownerSendTime = 500_000L;
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", null, null, staleStatuslineReply, 0);
        store.recordUserInput("session-one", ownerSendTime);
        Assert.assertEquals("the on-send input must overwrite the displayed reply before reconnect",
            Long.valueOf(ownerSendTime), store.effectiveReplyTimeMillis("session-one"));

        store.purgeSessionKeepingTheCallAndReplyTimes("session-one");

        Assert.assertEquals("an in-place reconnect must keep the on-send reply overwrite rather than "
                + "reverting to the minutes-old statusline reply",
            Long.valueOf(ownerSendTime), store.effectiveReplyTimeMillis("session-one"));
    }

    @Test
    public void preservingPurgeClearsThePendingCallToUserReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("session-one", 1_000L, "please review");
        Assert.assertFalse(store.getUnacknowledgedCallReasons("session-one").isEmpty());

        store.purgeSessionKeepingTheCallAndReplyTimes("session-one");

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
