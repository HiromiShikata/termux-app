package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityTierReplyAgeGrayTest {

    private static final long NOW_MILLIS = 1_000_000_000L;
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
    private static final long STALE_MILLIS = NOW_MILLIS - (9L * 60L * ONE_MINUTE_MILLIS);

    @Test
    public void staleOutWithRecentReplyIsYellow() {
        long replyEightMinutesAgo = NOW_MILLIS - (8L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_MILLIS, replyEightMinutesAgo, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void staleOutAndStaleReplyIsGray() {
        long replyElevenMinutesAgo = NOW_MILLIS - (11L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_MILLIS, replyElevenMinutesAgo, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void recentOutWithStaleReplyIsYellow() {
        long recentOut = NOW_MILLIS - ONE_MINUTE_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            recentOut, STALE_MILLIS, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void recentOutAndRecentReplyIsYellow() {
        long recentOut = NOW_MILLIS - ONE_MINUTE_MILLIS;
        long recentReply = NOW_MILLIS - (2L * ONE_MINUTE_MILLIS);

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            recentOut, recentReply, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void absentOutAndAbsentReplyIsNone() {
        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            null, null, null, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void pendingCallStaysRedRegardlessOfStaleOutAndReply() {
        long pendingCall = NOW_MILLIS - ONE_MINUTE_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            STALE_MILLIS, STALE_MILLIS, pendingCall, null, null, null, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }
}
