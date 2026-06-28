package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class SessionTimesLineTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long NOW = 1_000_000_000L;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static long timeMillis(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void showsCallOutAndReplyRelativeTimes() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 45L * ONE_SECOND_MILLIS,
            0,
            NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: 3h  out: 12m  reply: 45s  sub: 0", line.getText());
    }

    @Test
    public void showsMoreThanOneDayForAbsentCallButUnknownDashForAbsentReply() {
        SessionTimesLine line = SessionTimesLine.of(
            null, NOW - 12L * ONE_MINUTE_MILLIS, null, 0, NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: >1d  out: 12m  reply: -  sub: 0", line.getText());
    }

    @Test
    public void showsMoreThanOneDayForAFutureRolledOverTime() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW + ONE_HOUR_MILLIS, NOW - ONE_MINUTE_MILLIS, NOW, 0, NOW);

        Assert.assertEquals("call: >1d  out: 1m  reply: 0s  sub: 0", line.getText());
    }

    @Test
    public void staysVisibleAndShowsUnknownReplyDashWhenNoStatuslineTimesAreKnown() {
        SessionTimesLine line = SessionTimesLine.of(null, null, null, 0, NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: >1d  out: >1d  reply: -  sub: 0", line.getText());
    }

    @Test
    public void showsTheSubagentCountNextToTheTimes() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 45L * ONE_SECOND_MILLIS,
            4,
            NOW);

        Assert.assertEquals("call: 3h  out: 12m  reply: 45s  sub: 4", line.getText());
    }

    @Test
    public void rendersAShortReplyAcrossTheMidnightBoundaryNotMoreThanOneDay() {
        long now = timeMillis(2026, 6, 28, 0, 0, 10);
        ClaudeStatuslineTimes times = ClaudeStatuslineTimes.parse(
            "call:23:58:00 out:23:59:20 reply:23:59:30", now, UTC);

        SessionTimesLine line = SessionTimesLine.of(
            times.getCallTimeMillis(),
            times.getOutTimeMillis(),
            times.getReplyTimeMillis(),
            times.getSubagentCount(),
            now);

        Assert.assertEquals("call: 2m  out: 50s  reply: 40s  sub: 0", line.getText());
    }

    @Test
    public void rendersTheDayDisplayForAReplyTwentyFiveHoursAgo() {
        long now = timeMillis(2026, 6, 28, 12, 0, 0);
        long replyTwentyFiveHoursAgo = now - 25L * ONE_HOUR_MILLIS;

        SessionTimesLine line = SessionTimesLine.of(
            null, null, replyTwentyFiveHoursAgo, 0, now);

        Assert.assertEquals("call: >1d  out: >1d  reply: >1d  sub: 0", line.getText());
    }

    @Test
    public void rendersUnknownDashForReplyWithNoDataInsteadOfTheMisleadingDayDisplay() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS, NOW - 12L * ONE_MINUTE_MILLIS, null, 0, NOW);

        Assert.assertEquals("call: 3h  out: 12m  reply: -  sub: 0", line.getText());
    }

    @Test
    public void rendersTheRecentReplyTimeWhenAStatuslineReplyIsKnownAfterARestart() {
        SessionTimesLine line = SessionTimesLine.of(
            null, null, NOW - 2L * ONE_MINUTE_MILLIS, 0, NOW);

        Assert.assertEquals("call: >1d  out: >1d  reply: 2m  sub: 0", line.getText());
    }

    @Test
    public void columnAlignedLinePadsEachValueToAFixedWidthSoLabelsLineUp() {
        SessionTimesLine line = SessionTimesLine.ofColumnAligned(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 45L * ONE_SECOND_MILLIS,
            0,
            NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: 3h  out: 12m reply: 45s sub: 0  ", line.getText());
    }

    @Test
    public void columnAlignedLineRendersUnknownReplyDashPaddedToTheColumnWidth() {
        SessionTimesLine line = SessionTimesLine.ofColumnAligned(null, null, null, 0, NOW);

        Assert.assertEquals("call: >1d out: >1d reply: -   sub: 0  ", line.getText());
    }

    @Test
    public void columnAlignedLinePlacesEveryLabelAtTheSameColumnRegardlessOfValueLength() {
        SessionTimesLine shortValues = SessionTimesLine.ofColumnAligned(
            NOW - 5L * ONE_MINUTE_MILLIS, NOW - 5L * ONE_MINUTE_MILLIS, NOW - 5L * ONE_MINUTE_MILLIS, 0, NOW);
        SessionTimesLine longValues = SessionTimesLine.ofColumnAligned(null, null, null, 0, NOW);

        Assert.assertEquals(longValues.getText().indexOf("out:"), shortValues.getText().indexOf("out:"));
        Assert.assertEquals(longValues.getText().indexOf("reply:"), shortValues.getText().indexOf("reply:"));
        Assert.assertEquals(longValues.getText().indexOf("sub:"), shortValues.getText().indexOf("sub:"));
    }
}
