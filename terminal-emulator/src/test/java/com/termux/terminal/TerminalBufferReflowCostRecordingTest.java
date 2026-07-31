package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TerminalBufferReflowCostRecordingTest {

    private static final int TOTAL_ROWS = 100;
    private static final int SCREEN_ROWS = 24;
    private static final int COLUMNS = 80;

    @Test
    public void columnChangingResizeRecordsOneReflowSample() {
        TerminalBuffer buffer = new TerminalBuffer(COLUMNS, TOTAL_ROWS, SCREEN_ROWS);
        long samplesBefore = TerminalBufferReflowCostCounterHolder.getInstance().getSampleCount();

        buffer.resize(COLUMNS / 2, SCREEN_ROWS, TOTAL_ROWS, new int[]{0, 0}, TextStyle.NORMAL, false);

        Assert.assertEquals("A column-changing resize must record exactly one reflow sample, because the diagnostics"
                + " report attributes the perceived cost of a font size change to the number of reflows and their"
                + " duration",
            samplesBefore + 1, TerminalBufferReflowCostCounterHolder.getInstance().getSampleCount());
    }

    @Test
    public void rowOnlyResizeRecordsNoReflowSample() {
        TerminalBuffer buffer = new TerminalBuffer(COLUMNS, TOTAL_ROWS, SCREEN_ROWS);
        long samplesBefore = TerminalBufferReflowCostCounterHolder.getInstance().getSampleCount();

        buffer.resize(COLUMNS, SCREEN_ROWS - 4, TOTAL_ROWS, new int[]{0, 0}, TextStyle.NORMAL, false);

        Assert.assertEquals("A resize that keeps the column count must record no reflow sample, because it takes the"
                + " cheap path that does not walk the transcript, and counting it would dilute the measurement the"
                + " report exists to make",
            samplesBefore, TerminalBufferReflowCostCounterHolder.getInstance().getSampleCount());
    }

    @Test
    public void columnChangingResizePreservesTranscriptText() {
        TerminalBuffer buffer = new TerminalBuffer(COLUMNS, TOTAL_ROWS, SCREEN_ROWS);
        String text = "hello";
        for (int column = 0; column < text.length(); column++) {
            buffer.setChar(column, 0, text.charAt(column), TextStyle.NORMAL, null);
        }

        buffer.resize(COLUMNS / 2, SCREEN_ROWS, TOTAL_ROWS, new int[]{text.length(), 0}, TextStyle.NORMAL, false);

        Assert.assertEquals("Adding the reflow measurement must not change what the reflow produces, because this"
                + " change is only allowed to measure the terminal and never to alter it",
            text, buffer.getTranscriptText());
    }
}
