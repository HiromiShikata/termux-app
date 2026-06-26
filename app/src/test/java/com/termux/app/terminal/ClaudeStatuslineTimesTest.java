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
    public void aTimeAfterTheCurrentClockBelongsToTheCurrentDayAndIsInTheFuture() {
        long now = timeMillis(2026, 6, 26, 9, 0, 0);
        String screen = "call:23:30:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 23, 30, 0)),
            times.getCallTimeMillis());
        Assert.assertTrue(times.getCallTimeMillis() > now);
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
    public void usesTheLastOccurrenceWhenATokenAppearsMultipleTimes() {
        long now = timeMillis(2026, 6, 26, 14, 0, 0);
        String screen = "out:10:00:00 ... refreshed out:13:00:00";

        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(screen, now, UTC);

        Assert.assertEquals(Long.valueOf(timeMillis(2026, 6, 26, 13, 0, 0)),
            times.getOutTimeMillis());
    }
}
