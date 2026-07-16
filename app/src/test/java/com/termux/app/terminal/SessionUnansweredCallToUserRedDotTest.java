package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionUnansweredCallToUserRedDotTest {

    private static final String SESSION = "worker-session";
    private static final long TEN_MINUTES_MILLIS = 10L * 60L * 1000L;

    private static SessionNewActivityTier dotTier(SessionNewActivityStore store, long nowMillis) {
        return TermuxSessionsListViewController.newActivityIndicator(store, SESSION, nowMillis).getTier();
    }

    @Test
    public void explicitCallStaysRedOnTheDotWhenAPassiveStatuslineReplyOnlyEqualsTheCallAndOwnerNeverReplied() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000_000_000L;

        store.recordExplicitCall(SESSION, callMillis, "needs approval");
        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);

        long nowMillis = callMillis + TEN_MINUTES_MILLIS + 60_000L;

        Assert.assertEquals(SessionNewActivityTier.RED, dotTier(store, nowMillis));
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertFalse(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }

    @Test
    public void explicitCallClearsOutOfRedWhenTheOwnerActuallyReplies() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000_000_000L;

        store.recordExplicitCall(SESSION, callMillis, "needs approval");
        store.recordStatuslineTimes(SESSION, callMillis, callMillis, callMillis);

        long nowMillis = callMillis + TEN_MINUTES_MILLIS + 60_000L;
        Assert.assertEquals(SessionNewActivityTier.RED, dotTier(store, nowMillis));

        store.recordUserInput(SESSION, callMillis + 2_000L);

        Assert.assertNotEquals(SessionNewActivityTier.RED, dotTier(store, nowMillis));
        Assert.assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }

    @Test
    public void explicitCallClearsOutOfRedWhenAStatuslineReplyIsGenuinelyNewerThanTheCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000_000_000L;
        long replyMillis = callMillis + 120_000L;

        store.recordExplicitCall(SESSION, callMillis, "needs approval");
        store.recordStatuslineTimes(SESSION, callMillis, callMillis, replyMillis);

        long nowMillis = replyMillis + TEN_MINUTES_MILLIS + 60_000L;

        Assert.assertNotEquals(SessionNewActivityTier.RED, dotTier(store, nowMillis));
        Assert.assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }

    @Test
    public void genuinelyIdleSessionWithNoPendingCallStaysGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long callMillis = 1_000_000_000L;
        long replyMillis = callMillis + 60_000L;

        store.recordStatuslineTimes(SESSION, callMillis, callMillis, replyMillis);

        long nowMillis = replyMillis + TEN_MINUTES_MILLIS + 60_000L;

        Assert.assertEquals(SessionNewActivityTier.GRAY, dotTier(store, nowMillis));
    }
}
