package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ClaudeStatuslineTimesTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long timeMillis(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void parsesCallOutAndReplyTokensFromStatusline() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "model | call:13:45:30 out:13:59:00 reply:13:30:15 | branch";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertTrue(times.hasAnyToken());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 59, 0)),
            times.getOutTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 30, 15)),
            times.getReplyTimeMillis());
    }

    @Test
    public void hasNoTokensWhenStatuslineTokensAreAbsent() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);

        ClaudeStatuslineTimes times =
            ClaudeStatuslineTimes.parse("just an ordinary terminal line", now, UTC);

        Assert.assertFalse(times.hasAnyToken());
        Assert.assertNull(times.getCallTimeMillis());
        Assert.assertNull(times.getOutTimeMillis());
        Assert.assertNull(times.getReplyTimeMillis());
    }

    @Test
    public void parsesAvailableTokensWhenSomeAreMissing() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "status out:13:59:00 only";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertTrue(times.hasAnyToken());
        Assert.assertNull(times.getCallTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 59, 0)),
            times.getOutTimeMillis());
        Assert.assertNull(times.getReplyTimeMillis());
    }

    @Test
    public void aTimeAfterTheCurrentClockBelongsToThePreviousDayAndIsNeverInTheFuture() {
        long now = timeMillis(2026, 6, 26, 9, 0, 0);
        String screen = "call:23:30:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 25, 23, 30, 0)),
            times.getCallTimeMillis());
        Assert.assertTrue(times.getCallTimeMillis() <= now);
    }

    @Test
    public void aReplyTokenJustBeforeMidnightViewedJustAfterMidnightIsAboutFortySecondsAgo() {
        long now = timeMillis(2026, 6, 28, 0, 0, 10);
        String screen = "reply:23:59:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long replyTimeMillis = times.getReplyTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 59, 30)), replyTimeMillis);
        Assert.assertTrue(replyTimeMillis <= now);
        Assert.assertEquals("40s",
            SessionNewActivityStore.formatRelativeAge(replyTimeMillis, now));
    }

    @Test
    public void aReplyFiveMinutesBeforeMidnightViewedAfterMidnightRendersAsMinutes() {
        long now = timeMillis(2026, 6, 28, 0, 2, 0);
        String screen = "reply:23:57:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long replyTimeMillis = times.getReplyTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 57, 0)), replyTimeMillis);
        Assert.assertEquals("5m",
            SessionNewActivityStore.formatRelativeAge(replyTimeMillis, now));
    }

    @Test
    public void aTimeAtOrBeforeTheCurrentClockBelongsToTheCurrentDay() {
        long now = timeMillis(2026, 6, 28, 9, 0, 0);
        String screen = "reply:08:55:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long replyTimeMillis = times.getReplyTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 28, 8, 55, 0)), replyTimeMillis);
        Assert.assertEquals("5m",
            SessionNewActivityStore.formatRelativeAge(replyTimeMillis, now));
    }

    @Test
    public void anOutTokenJustBeforeMidnightViewedJustAfterMidnightIsAboutFiftySecondsAgo() {
        long now = timeMillis(2026, 6, 28, 0, 0, 10);
        String screen = "out:23:59:20";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long outTimeMillis = times.getOutTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 59, 20)), outTimeMillis);
        Assert.assertTrue(outTimeMillis <= now);
        Assert.assertEquals("50s",
            SessionNewActivityStore.formatRelativeAge(outTimeMillis, now));
    }

    @Test
    public void anOutTokenAtElevenPmViewedJustAfterMidnightIsAboutOneHourAgoNotMoreThanOneDay() {
        long now = timeMillis(2026, 6, 28, 0, 14, 0);
        String screen = "out:23:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long outTimeMillis = times.getOutTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 0, 0)), outTimeMillis);
        Assert.assertTrue(outTimeMillis <= now);
        Assert.assertEquals("1h",
            SessionNewActivityStore.formatRelativeAge(outTimeMillis, now));
    }

    @Test
    public void anOutTokenEarlierTodayBelongsToTodayAndRendersTheHourValue() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        String screen = "out:11:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Long outTimeMillis = times.getOutTimeMillis();
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 28, 11, 0, 0)), outTimeMillis);
        Assert.assertEquals("1h",
            SessionNewActivityStore.formatRelativeAge(outTimeMillis, now));
    }

    @Test
    public void anOutTimestampGenuinelyTwentyFiveHoursOldStillShowsTheDayDisplay() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        long outTwentyFiveHoursAgo = timeMillis(2026, 6, 27, 11, 0, 0);

        Assert.assertEquals(">1d",
            SessionNewActivityStore.formatRelativeAge(outTwentyFiveHoursAgo, now));
    }

    @Test
    public void ignoresOutOfRangeClockComponents() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);

        ClaudeStatuslineTimes times =
            ClaudeStatuslineTimes.parse("call:25:99:99", now, UTC);

        Assert.assertFalse(times.hasAnyToken());
    }

    @Test
    public void parsesTheSameTimesRegardlessOfSeparatorWhitespaceWidth() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        Long expectedCall = timeMillis(2026, 6, 26, 9, 15, 32);
        Long expectedOut = timeMillis(2026, 6, 26, 9, 37, 49);
        Long expectedReply = timeMillis(2026, 6, 26, 9, 36, 28);

        String singleSpace = "call:09:15:32 out:09:37:49 reply:09:36:28";
        String doubleSpace = "call:09:15:32  out:09:37:49  reply:09:36:28";
        String tabsAndMixed = "call:09:15:32\tout:09:37:49 \t reply:09:36:28";

        for (String screen : new String[] {singleSpace, doubleSpace, tabsAndMixed}) {
            ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);
            Assert.assertEquals(screen, expectedCall, times.getCallTimeMillis());
            Assert.assertEquals(screen, expectedOut, times.getOutTimeMillis());
            Assert.assertEquals(screen, expectedReply, times.getReplyTimeMillis());
        }
    }

    @Test
    public void parsesTokensWithSurroundingWhitespaceOnTheStatuslineRow() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "   call:09:15:32  out:09:37:49  reply:09:36:28   ";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 9, 15, 32)),
            times.getCallTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 9, 37, 49)),
            times.getOutTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 9, 36, 28)),
            times.getReplyTimeMillis());
    }

    @Test
    public void parsesTheSubagentCountFromTheSubToken() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "model | call:13:45:30 out:13:59:00 reply:13:30:15 SUB:3 | branch";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(3, times.getSubagentCount());
    }

    @Test
    public void subagentCountDefaultsToZeroWhenTheSubTokenIsAbsent() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "model | call:13:45:30 out:13:59:00 reply:13:30:15 | branch";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(0, times.getSubagentCount());
    }

    @Test
    public void aLoneSubTokenIsParsedAndCountsAsATokenPresence() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse("worker SUB:7 idle", now, UTC);

        Assert.assertTrue(times.hasAnyToken());
        Assert.assertEquals(7, times.getSubagentCount());
    }

    @Test
    public void usesTheLastOccurrenceWhenATokenAppearsMultipleTimes() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "out:10:00:00 ... refreshed out:13:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 0, 0)),
            times.getOutTimeMillis());
    }

    private static long timeMillisAt(TimeZone zone, int year, int month, int day,
                                     int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void aDatedTokenParsesToTheExactEmbeddedInstantInLocalTime() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "model | call:2026-06-26T13:45:30 | branch";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
    }

    @Test
    public void aDatedTokenIsInterpretedInTheProvidedLocalTimeZoneWhenNoOffsetIsPresent() {
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        long now = timeMillisAt(tokyo, 2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-06-26T13:45:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, tokyo);

        Assert.assertEquals(Long.valueOf(timeMillisAt(tokyo, 2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
        // The same wall-clock string in UTC is 9 hours later in absolute time than in Tokyo.
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30) - 9L * 3600L * 1000L),
            times.getCallTimeMillis());
    }

    @Test
    public void aDatedTokenBypassesTheMostRecentClockHeuristicAcrossMidnight() {
        // now is on 2026-06-28 but the embedded date is the previous day: the exact
        // embedded instant is used directly rather than being reconstructed from now.
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        String screen = "reply:2026-06-27T23:59:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 59, 30)),
            times.getReplyTimeMillis());
    }

    @Test
    public void aDatedTokenWithADateInThePastIsNotClampedToTheMostRecentDay() {
        // A genuinely old dated token (more than a day ago) keeps its embedded date,
        // whereas the time-only heuristic would have snapped it to the most recent day.
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        String screen = "out:2026-06-25T11:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 25, 11, 0, 0)),
            times.getOutTimeMillis());
    }

    @Test
    public void aTimeOnlyTokenStillUsesTheMostRecentClockHeuristic() {
        long now = timeMillis(2026, 6, 28, 0, 0, 10);
        String screen = "reply:23:59:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        // No embedded date, so the previous-day reconstruction still applies.
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 23, 59, 30)),
            times.getReplyTimeMillis());
    }

    @Test
    public void mixedDatedAndTimeOnlyTokensEachUseTheirOwnPath() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        // call is dated (previous day, used exactly), out is time-only (heuristic ->
        // today because 11:00 is before now), reply is dated on today.
        String screen = "call:2026-06-27T09:00:00 out:11:00:00 reply:2026-06-28T10:30:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 9, 0, 0)),
            times.getCallTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 28, 11, 0, 0)),
            times.getOutTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 28, 10, 30, 0)),
            times.getReplyTimeMillis());
    }

    @Test
    public void tolerateSpaceSeparatorInsteadOfTBetweenDateAndTime() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-06-26 13:45:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
    }

    @Test
    public void tolerateATrailingZuluZoneDesignatorAndTreatItAsUtc() {
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        long now = timeMillisAt(tokyo, 2026, 6, 26, 23, 0, 0);
        String screen = "call:2026-06-26T13:45:30Z";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, tokyo);

        // Z means UTC regardless of the provided local zone.
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
    }

    @Test
    public void tolerateAnExplicitPositiveZoneOffset() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-06-26T13:45:30+09:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        // 13:45:30+09:00 equals 04:45:30 UTC.
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 4, 45, 30)),
            times.getCallTimeMillis());
    }

    @Test
    public void tolerateAnExplicitNegativeZoneOffset() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-06-26T13:45:30-05:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        // 13:45:30-05:00 equals 18:45:30 UTC.
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 18, 45, 30)),
            times.getCallTimeMillis());
    }

    @Test
    public void tolerateFractionalSecondsWhichAreTruncatedToWholeSeconds() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-06-26T13:45:30.123 out:2026-06-26T13:50:00.5Z";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 45, 30)),
            times.getCallTimeMillis());
        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 50, 0)),
            times.getOutTimeMillis());
    }

    @Test
    public void aDatedTokenTakesPrecedenceOverAnEarlierTimeOnlyTokenForTheSameName() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        String screen = "out:11:00:00 ... refreshed out:2026-06-27T09:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 9, 0, 0)),
            times.getOutTimeMillis());
    }

    @Test
    public void usesTheLastDatedOccurrenceWhenADatedTokenAppearsMultipleTimes() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        String screen = "reply:2026-06-26T08:00:00 later reply:2026-06-27T09:30:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 27, 9, 30, 0)),
            times.getReplyTimeMillis());
    }

    @Test
    public void aDatedTokenWithAnInvalidCalendarDateIsIgnored() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "call:2026-13-40T13:45:30";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertNull(times.getCallTimeMillis());
        Assert.assertFalse(times.hasAnyToken());
    }
}
