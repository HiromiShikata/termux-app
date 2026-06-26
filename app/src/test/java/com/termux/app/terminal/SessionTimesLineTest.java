package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionTimesLineTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long NOW = 1_000_000_000L;

    @Test
    public void showsCallOutAndReplyRelativeTimes() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 45L * ONE_SECOND_MILLIS,
            NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: 3h  out: 12m  reply: 45s", line.getText());
    }

    @Test
    public void showsDashForAbsentValues() {
        SessionTimesLine line = SessionTimesLine.of(
            null, NOW - 12L * ONE_MINUTE_MILLIS, null, NOW);

        Assert.assertTrue(line.isVisible());
        Assert.assertEquals("call: -  out: 12m  reply: -", line.getText());
    }

    @Test
    public void showsMoreThanOneDayForAFutureRolledOverTime() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW + ONE_HOUR_MILLIS, NOW - ONE_MINUTE_MILLIS, NOW, NOW);

        Assert.assertEquals("call: >1 day  out: 1m  reply: 0s", line.getText());
    }

    @Test
    public void isNotVisibleWhenNoTimesAreKnown() {
        SessionTimesLine line = SessionTimesLine.of(null, null, null, NOW);

        Assert.assertFalse(line.isVisible());
        Assert.assertEquals("", line.getText());
    }
}
