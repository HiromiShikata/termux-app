package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionListYellowOutRecencyHonestyTest {

    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
    private static final long ONE_DAY_MILLIS = 24L * 60L * ONE_MINUTE_MILLIS;
    private static final long NOW_MILLIS = 10L * ONE_DAY_MILLIS;

    private static SessionNewActivityIndicator indicatorFor(SessionNewActivityStore store,
                                                            String sessionName, long nowMillis) {
        return TermuxSessionsListViewController.newActivityIndicator(store, sessionName, nowMillis);
    }

    @Test
    public void statuslineOutWithinThresholdIsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long recentOut = NOW_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes("worker", null, recentOut, null);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicatorFor(store, "worker", NOW_MILLIS).getTier());
    }

    @Test
    public void statuslineReplyWithinThresholdIsYellowEvenWhenOutIsStale() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        long recentReply = NOW_MILLIS - (5L * ONE_MINUTE_MILLIS);
        store.recordStatuslineTimes("worker", null, staleOut, recentReply);
        store.recordOutputActivity("worker", NOW_MILLIS - 1_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicatorFor(store, "worker", NOW_MILLIS).getTier());
    }

    @Test
    public void statuslineOutOlderThanOneDayIsNotYellowEvenWithRecentRawOutput() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes("worker", null, staleOut, null);
        store.recordOutputActivity("worker", NOW_MILLIS - 1_000L);

        SessionNewActivityTier tier = indicatorFor(store, "worker", NOW_MILLIS).getTier();

        Assert.assertNotEquals(SessionNewActivityTier.YELLOW, tier);
        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void parsedReplyAndOutBothOlderThanOneDayWithRecentRawOutputIsGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        long staleReply = NOW_MILLIS - ONE_DAY_MILLIS - (2L * ONE_MINUTE_MILLIS);
        store.recordStatuslineTimes("worker", null, staleOut, staleReply);
        store.recordOutputActivity("worker", NOW_MILLIS - 1_000L);

        Assert.assertEquals(SessionNewActivityTier.GRAY, indicatorFor(store, "worker", NOW_MILLIS).getTier());
    }

    @Test
    public void statuslineSessionWithoutOutTokenButStaleReplyIsGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleReply = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes("worker", null, null, staleReply);
        store.recordOutputActivity("worker", NOW_MILLIS - 1_000L);

        SessionNewActivityTier tier = indicatorFor(store, "worker", NOW_MILLIS).getTier();

        Assert.assertNotEquals(SessionNewActivityTier.YELLOW, tier);
        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void pendingCallStaysRedEvenWhenStatuslineOutIsStale() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes("worker", NOW_MILLIS - ONE_MINUTE_MILLIS, staleOut, null);
        store.recordExplicitCall("worker", NOW_MILLIS - ONE_MINUTE_MILLIS, "needs approval");

        Assert.assertEquals(SessionNewActivityTier.RED, indicatorFor(store, "worker", NOW_MILLIS).getTier());
    }

    @Test
    public void dotTierAgesOffTheSameStatuslineOutTimestampThatIsDisplayed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes("worker", null, staleOut, null);
        store.recordOutputActivity("worker", NOW_MILLIS - 1_000L);

        Long dotTierOut = store.outActivityTimeMillisForDotTier("worker");
        Long displayedOut = store.getStatuslineOutTimeMillis("worker");

        Assert.assertEquals(displayedOut, dotTierOut);
    }

    @Test
    public void replyTierAgesOffTheSameEffectiveReplyTimestampThatIsDisplayed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - ONE_DAY_MILLIS - ONE_MINUTE_MILLIS;
        long reply = NOW_MILLIS - (5L * ONE_MINUTE_MILLIS);
        store.recordStatuslineTimes("worker", null, staleOut, reply);

        Long dotTierReply = store.replyActivityTimeMillisForDotTier("worker");
        Long displayedReply = store.effectiveReplyTimeMillis("worker");

        Assert.assertEquals(displayedReply, dotTierReply);
    }

    @Test
    public void rawOnlySessionWithRecentOutputStillYellowSinceItHasNoStatusline() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity("worker", NOW_MILLIS - ONE_MINUTE_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicatorFor(store, "worker", NOW_MILLIS).getTier());
    }
}
