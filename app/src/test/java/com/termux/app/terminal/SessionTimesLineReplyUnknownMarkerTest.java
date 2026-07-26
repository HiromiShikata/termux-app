package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Reproduces https://github.com/HiromiShikata/termux-app/issues/1064
 *
 * Right after an app update, the OS clears the app cache directory, which
 * deletes the persisted SessionNewActivityStore JSON file. Until a session's
 * statusline is re-scanned, its reply time is unknown (represented in code
 * as a null replyTimeMillis). SessionTimesLine currently renders a null
 * reply the same way it renders a real timestamp that is genuinely more
 * than a day old: the ">1d" label. That makes an unknown reply
 * indistinguishable from a stale one, which is the misleading ">1d" seen
 * on most sessions immediately after an update.
 *
 * The reply column must instead render a distinct unknown marker ("-")
 * when replyTimeMillis is null, while a real reply timestamp at or beyond
 * one day old must still render ">1d". The call and out columns are
 * unchanged by this requirement.
 */
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
