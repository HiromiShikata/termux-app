package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionTimesLineReplyUnknownMarkerTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;
    private static final long NOW = 1_000_000_000L;

    @Test
    public void rendersUnknownMarkerNotMoreThanOneDayWhenReplyTimeIsUnknown() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            null,
            0,
            NOW);

        Assert.assertEquals(
            "call: 3h  out: 12m  reply: -  sub: 0",
            line.getText());
    }

    @Test
    public void stillRendersMoreThanOneDayForAGenuinelyOldReplyTimestamp() {
        SessionTimesLine line = SessionTimesLine.of(
            NOW - 3L * ONE_HOUR_MILLIS,
            NOW - 12L * ONE_MINUTE_MILLIS,
            NOW - 2L * ONE_DAY_MILLIS,
            0,
            NOW);

        Assert.assertEquals(
            "call: 3h  out: 12m  reply: >1d  sub: 0",
            line.getText());
    }
}
