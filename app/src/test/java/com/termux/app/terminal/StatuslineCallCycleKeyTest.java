package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class StatuslineCallCycleKeyTest {

    private static final String MARKER = "🔴";

    private static String paneText(String callClock, String outClock, String replyClock) {
        return "[claude] out:" + outClock + " call:" + callClock + " reply:" + replyClock + " SUB:0\n"
            + "<call-to-user>" + MARKER + "</call-to-user>\n";
    }

    private static String keyForFirstTagIn(String output) {
        int tagStart = output.indexOf("<call-to-user>");
        return StatuslineCallCycleKey.resolve(MARKER, output, tagStart);
    }

    @Test
    public void keyIsTheValueAloneWhenNoStatuslineTokenPrecedesTheTag() {
        assertEquals(MARKER, keyForFirstTagIn("plain work output\n<call-to-user>" + MARKER
            + "</call-to-user>\n"));
    }

    @Test
    public void keyIsTheValueAloneWhenTheTagStartsTheOutput() {
        assertEquals(MARKER, StatuslineCallCycleKey.resolve(MARKER,
            "<call-to-user>" + MARKER + "</call-to-user>", 0));
    }

    @Test
    public void keyIsTheValueAloneWhenTheOutputIsNull() {
        assertEquals(MARKER, StatuslineCallCycleKey.resolve(MARKER, null, 12));
    }

    @Test
    public void identicalPaneTextYieldsAnIdenticalKey() {
        String output = paneText("10:01:00", "10:01:00", "10:00:30");

        assertEquals(keyForFirstTagIn(output), keyForFirstTagIn(output));
    }

    @Test
    public void aLaterCallTokenYieldsADifferentKeyForTheSameConstantMarker() {
        assertNotEquals(keyForFirstTagIn(paneText("10:01:00", "10:01:00", "10:00:30")),
            keyForFirstTagIn(paneText("10:02:00", "10:02:00", "10:01:30")));
    }

    @Test
    public void aLaterReplyTokenYieldsADifferentKeyWhenTheCallTokenRepeats() {
        assertNotEquals(keyForFirstTagIn(paneText("10:01:00", "10:01:00", "10:00:30")),
            keyForFirstTagIn(paneText("10:01:00", "10:01:00", "10:01:30")));
    }

    @Test
    public void onlyTokensRenderedBeforeTheTagParticipateInTheKey() {
        String beforeOnly = "[claude] out:10:01:00 call:10:01:00 reply:10:00:30\n"
            + "<call-to-user>" + MARKER + "</call-to-user>\n";
        String withLaterStatusline = beforeOnly
            + "[claude] out:10:03:00 call:10:03:00 reply:10:02:30\n";

        assertEquals(keyForFirstTagIn(beforeOnly), keyForFirstTagIn(withLaterStatusline));
    }

    @Test
    public void theNearestPrecedingStatuslineDefinesTheKey() {
        String twoStatuslines = "[claude] out:10:01:00 call:10:01:00 reply:10:00:30\n"
            + "work output\n"
            + "[claude] out:10:02:00 call:10:02:00 reply:10:01:30\n"
            + "<call-to-user>" + MARKER + "</call-to-user>\n";

        assertEquals(keyForFirstTagIn(paneText("10:02:00", "10:02:00", "10:01:30")),
            keyForFirstTagIn(twoStatuslines));
    }

    @Test
    public void aDatedStatuslineTokenParticipatesInTheKey() {
        String datedPane = "[claude] out:2026-07-30T10:01:00 call:2026-07-30T10:01:00"
            + " reply:2026-07-30T10:00:30\n"
            + "<call-to-user>" + MARKER + "</call-to-user>\n";
        String nextDayPane = "[claude] out:2026-07-31T10:01:00 call:2026-07-31T10:01:00"
            + " reply:2026-07-31T10:00:30\n"
            + "<call-to-user>" + MARKER + "</call-to-user>\n";

        assertNotEquals(keyForFirstTagIn(datedPane), keyForFirstTagIn(nextDayPane));
    }

    @Test
    public void aDifferentMarkerUnderTheSameStatuslineYieldsADifferentKey() {
        String output = paneText("10:01:00", "10:01:00", "10:00:30");
        int tagStart = output.indexOf("<call-to-user>");

        assertNotEquals(StatuslineCallCycleKey.resolve(MARKER, output, tagStart),
            StatuslineCallCycleKey.resolve("needs approval", output, tagStart));
    }
}
