package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CallToUserTagScrollLocatorTest {

    @Test
    public void returnsExternalRowOfTagWithinVisibleScreen() {
        List<String> rowTexts = Arrays.asList(
            "first output line",
            "<call-to-user>needs approval</call-to-user>",
            "trailing output line");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, 0);

        assertEquals(1, externalRow);
    }

    @Test
    public void mapsTranscriptRowsToNegativeExternalIndices() {
        List<String> rowTexts = Arrays.asList(
            "scrollback line a",
            "<call-to-user>review now</call-to-user>",
            "screen line one",
            "screen line two");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, -2);

        assertEquals(-1, externalRow);
    }

    @Test
    public void returnsRowOfMostRecentTagWhenMultipleArePresent() {
        List<String> rowTexts = Arrays.asList(
            "<call-to-user>older request</call-to-user>",
            "intervening output",
            "<call-to-user>newest request</call-to-user>");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, -2);

        assertEquals(0, externalRow);
    }

    @Test
    public void findsTagThatWrapsAcrossTwoVisualRows() {
        List<String> rowTexts = Arrays.asList(
            "preceding output line",
            "blah blah blah blah <call-to",
            "-user>please review</call-to-user>");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, 0);

        assertEquals(1, externalRow);
    }

    @Test
    public void returnsSentinelWhenNoTagPresent() {
        List<String> rowTexts = Arrays.asList("plain output", "more plain output");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, -1);

        assertEquals(CallToUserTagScrollLocator.NO_TAG_ROW, externalRow);
    }

    @Test
    public void returnsSentinelForEmptyTranscript() {
        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(
            Collections.emptyList(), 0);

        assertEquals(CallToUserTagScrollLocator.NO_TAG_ROW, externalRow);
    }

    @Test
    public void clampsTargetAboveBottomToZero() {
        assertEquals(0, CallToUserTagScrollLocator.clampTopRow(3, 100));
    }

    @Test
    public void clampsTargetBeyondOldestTranscriptRowToTranscriptTop() {
        assertEquals(-100, CallToUserTagScrollLocator.clampTopRow(-250, 100));
    }

    @Test
    public void keepsTargetWithinTranscriptRange() {
        assertEquals(-40, CallToUserTagScrollLocator.clampTopRow(-40, 100));
    }

    @Test
    public void scrollTargetComposesLocateAndClamp() {
        List<String> rowTexts = Arrays.asList(
            "scrollback line a",
            "scrollback line b",
            "<call-to-user>review now</call-to-user>",
            "screen line one");

        int targetTopRow = CallToUserTagScrollLocator.scrollTargetTopRow(rowTexts, -3, 3);

        assertEquals(-1, targetTopRow);
    }

    @Test
    public void scrollTargetReturnsSentinelWhenNoTagPresent() {
        List<String> rowTexts = Arrays.asList("plain", "output");

        int targetTopRow = CallToUserTagScrollLocator.scrollTargetTopRow(rowTexts, -1, 1);

        assertEquals(CallToUserTagScrollLocator.NO_TAG_ROW, targetTopRow);
    }
}
