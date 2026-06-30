package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class SessionNewActivityStoreClockAliasTierTest {

    private static final String SESSION = "long-idle-session";

    private static long timeMillis(TimeZone timeZone, int year, int month, int day,
                                   int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void aRescanThatReResolvesTheSameClockTimeToTodayDoesNotMakeAStaleSessionYellow() {
        TimeZone timeZone = TimeZone.getDefault();
        long genuineOutThreeDaysAgo = timeMillis(timeZone, 2026, 6, 25, 13, 55, 0);
        long aliasedOutToday = timeMillis(timeZone, 2026, 6, 28, 13, 55, 0);
        long nowToday = timeMillis(timeZone, 2026, 6, 28, 14, 0, 0);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION, null, genuineOutThreeDaysAgo, null);

        store.recordStatuslineTimes(SESSION, null, aliasedOutToday, null);

        Assert.assertEquals(Long.valueOf(genuineOutThreeDaysAgo),
            store.getStatuslineOutTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.GRAY, store.tierFor(SESSION, nowToday));
        Assert.assertEquals(SessionNewActivityStore.MORE_THAN_ONE_DAY_LABEL,
            SessionNewActivityStore.formatRelativeAge(genuineOutThreeDaysAgo, nowToday));
    }

    @Test
    public void aGenuineFreshOutWithinTenMinutesIsYellow() {
        TimeZone timeZone = TimeZone.getDefault();
        long genuineOutThreeDaysAgo = timeMillis(timeZone, 2026, 6, 25, 13, 55, 0);
        long genuineFreshOut = timeMillis(timeZone, 2026, 6, 28, 13, 56, 0);
        long nowToday = timeMillis(timeZone, 2026, 6, 28, 14, 0, 0);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION, null, genuineOutThreeDaysAgo, null);

        store.recordStatuslineTimes(SESSION, null, genuineFreshOut, null);

        Assert.assertEquals(Long.valueOf(genuineFreshOut), store.getStatuslineOutTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(SESSION, nowToday));
    }

    @Test
    public void aReplyClockAliasDoesNotReviveAStaleSessionToYellow() {
        TimeZone timeZone = TimeZone.getDefault();
        long genuineReplyThreeDaysAgo = timeMillis(timeZone, 2026, 6, 25, 13, 55, 0);
        long genuineOutThreeDaysAgo = timeMillis(timeZone, 2026, 6, 25, 13, 50, 0);
        long aliasedReplyToday = timeMillis(timeZone, 2026, 6, 28, 13, 55, 0);
        long nowToday = timeMillis(timeZone, 2026, 6, 28, 14, 0, 0);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION, null, genuineOutThreeDaysAgo, genuineReplyThreeDaysAgo);

        store.recordStatuslineTimes(SESSION, null, null, aliasedReplyToday);

        Assert.assertEquals(Long.valueOf(genuineReplyThreeDaysAgo),
            store.getStatuslineReplyTimeMillis(SESSION));
        Assert.assertEquals(SessionNewActivityTier.GRAY, store.tierFor(SESSION, nowToday));
    }
}
