package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SessionInfoBlockTest {

    @Test
    public void liveTerminalTitleIsTheLastLineOfTheInfoBlockWhenEveryPartIsPresent() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago    out: 1m ago    seen: 1m ago", "definition title",
            "live terminal title");

        List<SessionInfoLine> orderedLines = block.orderedNonEmptyLines();

        Assert.assertEquals(SessionInfoLine.SESSION_TITLE,
            orderedLines.get(orderedLines.size() - 1));
    }

    @Test
    public void liveTerminalTitleSitsBelowEveryOtherInfoLineSoItReadsAtTheBottomOfTheRow() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago    out: 1m ago    seen: 1m ago", "definition title",
            "live terminal title");

        int sessionTitleStart = block.startOf(SessionInfoLine.SESSION_TITLE);

        Assert.assertTrue(sessionTitleStart > block.startOf(SessionInfoLine.SESSION_NAME));
        Assert.assertTrue(sessionTitleStart > block.startOf(SessionInfoLine.TIMESTAMP));
        Assert.assertTrue(sessionTitleStart > block.startOf(SessionInfoLine.DEFINITION_TITLE));
    }

    @Test
    public void liveTerminalTitleStaysAtTheBottomEvenWhenOnlyTheSessionNameAndTitleArePresent() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "", "", "live terminal title");

        List<SessionInfoLine> orderedLines = block.orderedNonEmptyLines();

        Assert.assertEquals(2, orderedLines.size());
        Assert.assertEquals(SessionInfoLine.SESSION_NAME, orderedLines.get(0));
        Assert.assertEquals(SessionInfoLine.SESSION_TITLE, orderedLines.get(1));
    }

    @Test
    public void composedTextJoinsNonEmptyLinesWithNewlinesInCanonicalOrder() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago", "definition title", "live terminal title");

        Assert.assertEquals(
            "worker-1\ncall: 1m ago\ndefinition title\nlive terminal title",
            block.text());
    }

    @Test
    public void emptyPartsAreOmittedAndReportNegativeOffsets() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "", "", "");

        Assert.assertEquals("worker-1", block.text());
        Assert.assertEquals(0, block.startOf(SessionInfoLine.SESSION_NAME));
        Assert.assertEquals(-1, block.startOf(SessionInfoLine.TIMESTAMP));
        Assert.assertEquals(-1, block.startOf(SessionInfoLine.SESSION_TITLE));
    }

    @Test
    public void offsetsPointToTheActualSubstringForEachLine() {
        SessionInfoBlock block = SessionInfoBlock.compose("", "worker-1",
            "call: 1m ago", "definition title", "live terminal title");

        String text = block.text();

        Assert.assertEquals("worker-1",
            text.substring(block.startOf(SessionInfoLine.SESSION_NAME), block.endOf(SessionInfoLine.SESSION_NAME)));
        Assert.assertEquals("live terminal title",
            text.substring(block.startOf(SessionInfoLine.SESSION_TITLE),
                block.endOf(SessionInfoLine.SESSION_TITLE)));
    }
}
