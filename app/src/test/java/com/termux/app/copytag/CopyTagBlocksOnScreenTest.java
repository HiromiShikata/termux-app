package com.termux.app.copytag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CopyTagBlocksOnScreenTest {

    private static ScreenRow row(String text) {
        return new ScreenRow(text, false);
    }

    private static ScreenRow wrappedRow(String text) {
        return new ScreenRow(text, true);
    }

    private static CopyTagBlocksOnScreen screenOf(List<ScreenRow> rows) {
        return new CopyTagBlocksOnScreen(rows, 0);
    }

    @Test
    public void tappingARowInsideABlockReturnsEveryLineOfThatBlock() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("before the block"),
            row("<copy>"),
            row("first line"),
            row("second line"),
            row("</copy>"),
            row("after the block")));

        assertEquals("first line\nsecond line", screen.contentOfTheBlockCoveringRow(2));
        assertEquals("first line\nsecond line", screen.contentOfTheBlockCoveringRow(3));
    }

    @Test
    public void tappingTheRowHoldingTheOpeningTagReturnsTheBlock() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row("the payload"),
            row("</copy>")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(0));
        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(2));
    }

    @Test
    public void tappingARowOutsideEveryBlockReturnsNothing() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("plain output"),
            row("<copy>"),
            row("the payload"),
            row("</copy>"),
            row("more plain output")));

        assertNull(screen.contentOfTheBlockCoveringRow(0));
        assertNull(screen.contentOfTheBlockCoveringRow(4));
    }

    @Test
    public void aRowThatWrapsOntoTheNextIsJoinedWithoutANewline() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            wrappedRow("a very long value that ran off"),
            row(" the edge of the screen"),
            row("</copy>")));

        assertEquals("a very long value that ran off the edge of the screen",
            screen.contentOfTheBlockCoveringRow(1));
    }

    @Test
    public void blankLinesAroundTheContentAreTrimmed() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row(""),
            row("the payload"),
            row(""),
            row("</copy>")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(2));
    }

    @Test
    public void contentSharingTheRowWithTheTagsIsReturnedWithoutThem() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("prompt <copy>the payload</copy> trailing")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(0));
    }

    @Test
    public void aBlockLeftUnclosedIsNotOfferedForCopying() {
        CopyTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row("still being written")));

        assertNull(screen.contentOfTheBlockCoveringRow(1));
    }

    @Test
    public void rowsAreAddressedByTheirTerminalRowNumber() {
        CopyTagBlocksOnScreen screen = new CopyTagBlocksOnScreen(Arrays.asList(
            row("<copy>"),
            row("the payload"),
            row("</copy>")), -3);

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(-2));
        assertNull(screen.contentOfTheBlockCoveringRow(5));
    }
}
