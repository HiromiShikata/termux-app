package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SessionInfoBlockTest {

    @Test
    public void callToUserReasonIsTheLastLineOfTheInfoBlockWhenEveryPartIsPresent() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago    out: 1m ago    seen: 1m ago", "definition title",
            "live terminal title", "⚠ needs your reply");

        List<SessionInfoLine> orderedLines = block.orderedNonEmptyLines();

        Assert.assertEquals(SessionInfoLine.EXPLICIT_CALL_REASON,
            orderedLines.get(orderedLines.size() - 1));
    }

    @Test
    public void callToUserReasonSitsBelowEveryOtherInfoLineSoItReadsAtTheBottomOfTheRow() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago    out: 1m ago    seen: 1m ago", "definition title",
            "live terminal title", "⚠ needs your reply");

        int reasonStart = block.startOf(SessionInfoLine.EXPLICIT_CALL_REASON);

        Assert.assertTrue(reasonStart > block.startOf(SessionInfoLine.SESSION_NAME));
        Assert.assertTrue(reasonStart > block.startOf(SessionInfoLine.TIMESTAMP));
        Assert.assertTrue(reasonStart > block.startOf(SessionInfoLine.DEFINITION_TITLE));
        Assert.assertTrue(reasonStart > block.startOf(SessionInfoLine.SESSION_TITLE));
    }

    @Test
    public void callToUserReasonStaysAtTheBottomEvenWhenOnlyTheSessionNameAndReasonArePresent() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "", "", "", "⚠ needs your reply");

        List<SessionInfoLine> orderedLines = block.orderedNonEmptyLines();

        Assert.assertEquals(2, orderedLines.size());
        Assert.assertEquals(SessionInfoLine.SESSION_NAME, orderedLines.get(0));
        Assert.assertEquals(SessionInfoLine.EXPLICIT_CALL_REASON, orderedLines.get(1));
    }

    @Test
    public void composedTextJoinsNonEmptyLinesWithNewlinesInCanonicalOrder() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago", "definition title", "live terminal title", "⚠ needs your reply");

        Assert.assertEquals(
            "worker-1\ncall: 1m ago\ndefinition title\nlive terminal title\n⚠ needs your reply",
            block.text());
    }

    @Test
    public void emptyPartsAreOmittedAndReportNegativeOffsets() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "", "", "", "");

        Assert.assertEquals("worker-1", block.text());
        Assert.assertEquals(0, block.startOf(SessionInfoLine.SESSION_NAME));
        Assert.assertEquals(-1, block.startOf(SessionInfoLine.TIMESTAMP));
        Assert.assertEquals(-1, block.startOf(SessionInfoLine.EXPLICIT_CALL_REASON));
    }

    @Test
    public void offsetsPointToTheActualSubstringForEachLine() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago", "definition title", "live terminal title", "⚠ needs your reply");

        String text = block.text();

        Assert.assertEquals("worker-1",
            text.substring(block.startOf(SessionInfoLine.SESSION_NAME), block.endOf(SessionInfoLine.SESSION_NAME)));
        Assert.assertEquals("⚠ needs your reply",
            text.substring(block.startOf(SessionInfoLine.EXPLICIT_CALL_REASON),
                block.endOf(SessionInfoLine.EXPLICIT_CALL_REASON)));
    }
}
