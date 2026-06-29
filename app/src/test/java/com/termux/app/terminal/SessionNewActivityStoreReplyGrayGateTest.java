package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStoreReplyGrayGateTest {

    private static final String SESSION = "long-idle-session";
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
    private static final long NOW_MILLIS = 1_000_000_000L;
    private static final long NINE_HOURS_MILLIS = 9L * 60L * ONE_MINUTE_MILLIS;

    @Test
    public void longIdleSessionThatWasRedDoesNotGoGrayTheInstantTheOwnerRepliesOnce() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - NINE_HOURS_MILLIS;
        long oldReply = staleOut - ONE_MINUTE_MILLIS;
        long olderCall = staleOut;
        store.recordStatuslineTimes(SESSION, olderCall, staleOut, oldReply);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION, NOW_MILLIS));

        store.recordUserInput(SESSION, NOW_MILLIS);

        SessionNewActivityTier tierAfterReply = store.tierFor(SESSION, NOW_MILLIS);
        Assert.assertNotEquals(SessionNewActivityTier.GRAY, tierAfterReply);
        Assert.assertEquals(SessionNewActivityTier.YELLOW, tierAfterReply);
    }

    @Test
    public void sessionGoesGrayOnceTheReplyItselfAgesPastTenMinutes() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - NINE_HOURS_MILLIS;
        long replyEleven = NOW_MILLIS - (11L * ONE_MINUTE_MILLIS);
        long earlierCall = replyEleven - ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes(SESSION, earlierCall, staleOut, replyEleven);

        Assert.assertEquals(SessionNewActivityTier.GRAY, store.tierFor(SESSION, NOW_MILLIS));
    }

    @Test
    public void freshReplyKeepsTheBottomSheetIndicatorOutOfGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        long staleOut = NOW_MILLIS - NINE_HOURS_MILLIS;
        long oldReply = staleOut - ONE_MINUTE_MILLIS;
        long olderCall = staleOut;
        store.recordStatuslineTimes(SESSION, olderCall, staleOut, oldReply);
        store.recordUserInput(SESSION, NOW_MILLIS);

        SessionNewActivityIndicator indicator =
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION, NOW_MILLIS);

        Assert.assertNotEquals(SessionNewActivityTier.GRAY, indicator.getTier());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
    }
}
