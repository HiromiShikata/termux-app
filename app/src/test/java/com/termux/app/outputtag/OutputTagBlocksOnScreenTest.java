package com.termux.app.outputtag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OutputTagBlocksOnScreenTest {

    private static final String COPY_TAG_NAME = "copy";

    private static ScreenRow row(String text) {
        return new ScreenRow(text, false);
    }

    private static ScreenRow wrappedRow(String text) {
        return new ScreenRow(text, true);
    }

    private static OutputTagBlocksOnScreen screenOf(List<ScreenRow> rows) {
        return OutputTagBlocksOnScreen.of(COPY_TAG_NAME, rows, 0);
    }

    @Test
    public void theTagNameDecidesWhichBlocksAreFound() {
        List<ScreenRow> rows = Arrays.asList(
            row("<open>"),
            row("https://example.com/tagged"),
            row("</open>"));

        assertEquals("https://example.com/tagged",
            OutputTagBlocksOnScreen.of("open", rows, 0).contentOfTheBlockCoveringRow(1));
        assertNull("a block of another tag must stay invisible to this scanner",
            OutputTagBlocksOnScreen.of(COPY_TAG_NAME, rows, 0).contentOfTheBlockCoveringRow(1));
    }

    @Test
    public void tappingARowInsideABlockReturnsEveryLineOfThatBlock() {
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
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
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row("the payload"),
            row("</copy>")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(0));
        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(2));
    }

    @Test
    public void tappingARowOutsideEveryBlockReturnsNothing() {
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
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
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            wrappedRow("a very long value that ran off"),
            row(" the edge of the screen"),
            row("</copy>")));

        assertEquals("a very long value that ran off the edge of the screen",
            screen.contentOfTheBlockCoveringRow(1));
    }

    @Test
    public void blankLinesAroundTheContentAreTrimmed() {
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row(""),
            row("the payload"),
            row(""),
            row("</copy>")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(2));
    }

    @Test
    public void contentSharingTheRowWithTheTagsIsReturnedWithoutThem() {
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("prompt <copy>the payload</copy> trailing")));

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(0));
    }

    @Test
    public void aBlockLeftUnclosedIsNotOfferedForCopying() {
        OutputTagBlocksOnScreen screen = screenOf(Arrays.asList(
            row("<copy>"),
            row("still being written")));

        assertNull(screen.contentOfTheBlockCoveringRow(1));
    }

    @Test
    public void rowsAreAddressedByTheirTerminalRowNumber() {
        OutputTagBlocksOnScreen screen = OutputTagBlocksOnScreen.of(COPY_TAG_NAME, Arrays.asList(
            row("<copy>"),
            row("the payload"),
            row("</copy>")), -3);

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(-2));
        assertNull(screen.contentOfTheBlockCoveringRow(5));
    }

    @Test
    public void aTapInsideASmallBlockReadsOnlyTheRowsOfThatBlock() {
        CountingScreenRows rows = new CountingScreenRows(rowNumber -> {
            if (rowNumber == -1) return row("<copy>");
            if (rowNumber == 0) return row("the payload");
            if (rowNumber == 1) return row("</copy>");
            return row("unrelated output");
        });

        OutputTagBlocksOnScreen screen = new OutputTagBlocksOnScreen(COPY_TAG_NAME, rows, -400, 400);

        assertEquals("the payload", screen.contentOfTheBlockCoveringRow(0));
        assertEquals(3, rows.numberOfRowsRead());
    }

    @Test
    public void aTapBelowAClosedBlockStopsReadingAtThatBlocksClosingTag() {
        CountingScreenRows rows = new CountingScreenRows(rowNumber ->
            rowNumber == -1 ? row("</copy>") : row("unrelated output"));

        OutputTagBlocksOnScreen screen = new OutputTagBlocksOnScreen(COPY_TAG_NAME, rows, -400, 400);

        assertNull(screen.contentOfTheBlockCoveringRow(0));
        assertEquals(2, rows.numberOfRowsRead());
    }

    private static final class CountingScreenRows implements ScreenRows {

        private final ScreenRows delegate;

        private int rowsRead;

        private CountingScreenRows(ScreenRows delegate) {
            this.delegate = delegate;
        }

        @Override
        public ScreenRow rowAt(int rowNumber) {
            rowsRead++;
            return delegate.rowAt(rowNumber);
        }

        private int numberOfRowsRead() {
            return rowsRead;
        }
    }
}
