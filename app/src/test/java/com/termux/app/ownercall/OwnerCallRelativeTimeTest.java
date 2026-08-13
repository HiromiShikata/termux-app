package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OwnerCallRelativeTimeTest {

    private static final long NOW = 1_800_000_000_000L;

    private static String elapsed(long seconds) {
        return OwnerCallRelativeTime.of(NOW - seconds * 1000L, NOW);
    }

    @Test
    public void countsInSecondsWithinTheFirstMinute() {
        assertEquals("0秒前", elapsed(0));
        assertEquals("45秒前", elapsed(45));
        assertEquals("59秒前", elapsed(59));
    }

    @Test
    public void countsInMinutesFromOneMinuteToOneHour() {
        assertEquals("1分前", elapsed(60));
        assertEquals("6分前", elapsed(6 * 60));
        assertEquals("59分前", elapsed(59 * 60 + 59));
    }

    @Test
    public void countsInHoursFromOneHourOnward() {
        assertEquals("1時間前", elapsed(60 * 60));
        assertEquals("3時間前", elapsed(3 * 60 * 60 + 59 * 60));
        assertEquals("50時間前", elapsed(50 * 60 * 60));
    }

    @Test
    public void treatsACallTimeInTheFutureAsJustNow() {
        assertEquals("0秒前", OwnerCallRelativeTime.of(NOW + 5000L, NOW));
    }
}
