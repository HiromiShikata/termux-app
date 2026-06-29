package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityTierReplyAgeGrayTest {

    private static final long NOW_MILLIS = 1_000_000_000L;
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
    private static final long STALE_OUTPUT_MILLIS = NOW_MILLIS - (9L * 60L * ONE_MINUTE_MILLIS);

    @Test
    public void replyYoungerThanTenMinutesIsNotGrayEvenWhenOutputIsStale() {
        long replyEightMinutesAgo = NOW_MILLIS - (8L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, null, null, null, null, replyEightMinutesAgo, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void replyOlderThanTenMinutesIsGrayWhenOutputIsStale() {
        long replyElevenMinutesAgo = NOW_MILLIS - (11L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, null, null, null, null, replyElevenMinutesAgo, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void replyExactlyTenMinutesAgoIsStillNotGrayBecauseTheFreshnessBoundaryIsInclusive() {
        long replyAtFreshnessBoundary = NOW_MILLIS - SessionNewActivityTier.REPLY_FRESH_MAX_AGE_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, null, null, null, null, replyAtFreshnessBoundary, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void grayOneMillisecondPastTheTenMinuteReplyFreshnessBoundary() {
        long replyJustPastFreshnessBoundary =
            NOW_MILLIS - SessionNewActivityTier.REPLY_FRESH_MAX_AGE_MILLIS - 1L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, null, null, null, null, replyJustPastFreshnessBoundary, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void absentReplyKeepsTheStaleOutputGrayBehaviorForRawOnlySessions() {
        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, null, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void recentOutputStaysYellowRegardlessOfReplyAge() {
        long recentOutput = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long replyAnHourAgo = NOW_MILLIS - (60L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            recentOutput, null, null, null, null, replyAnHourAgo, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void pendingCallStaysRedRegardlessOfReplyFreshness() {
        long pendingCall = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long replyFiveMinutesAgo = NOW_MILLIS - (5L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_OUTPUT_MILLIS, pendingCall, null, null, null, replyFiveMinutesAgo, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }
}
