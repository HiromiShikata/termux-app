package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ClockAliasedStatuslineTimeGuardTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long timeMillis(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void sameClockTimeOnALaterDayIsAClockAlias() {
        long stored = timeMillis(2026, 6, 25, 13, 55, 0);
        long candidate = timeMillis(2026, 6, 28, 13, 55, 0);

        Assert.assertTrue(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, candidate, UTC));
    }

    @Test
    public void aGenuineLaterTimeOnTheSameDayIsNotAClockAlias() {
        long stored = timeMillis(2026, 6, 28, 13, 55, 0);
        long candidate = timeMillis(2026, 6, 28, 14, 10, 0);

        Assert.assertFalse(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, candidate, UTC));
    }

    @Test
    public void aDifferentClockTimeOnALaterDayIsNotAClockAlias() {
        long stored = timeMillis(2026, 6, 25, 13, 55, 0);
        long candidate = timeMillis(2026, 6, 28, 13, 56, 0);

        Assert.assertFalse(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, candidate, UTC));
    }

    @Test
    public void aCandidateAtOrBeforeTheStoredTimeIsNeverAClockAlias() {
        long stored = timeMillis(2026, 6, 28, 13, 55, 0);
        long earlierCandidate = timeMillis(2026, 6, 28, 13, 50, 0);

        Assert.assertFalse(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, earlierCandidate, UTC));
        Assert.assertFalse(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, stored, UTC));
    }

    @Test
    public void sameClockTimeExactlyOneDayLaterIsAClockAlias() {
        long stored = timeMillis(2026, 6, 27, 9, 0, 0);
        long candidate = timeMillis(2026, 6, 28, 9, 0, 0);

        Assert.assertTrue(
            ClockAliasedStatuslineTimeGuard.isOlderDayWithSameClockTime(stored, candidate, UTC));
    }
}
