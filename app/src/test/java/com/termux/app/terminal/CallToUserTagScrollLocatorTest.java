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
            "<call-to-user-pending>needs approval</call-to-user-pending>",
            "trailing output line");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, 0);

        assertEquals(1, externalRow);
    }

    @Test
    public void mapsTranscriptRowsToNegativeExternalIndices() {
        List<String> rowTexts = Arrays.asList(
            "scrollback line a",
            "<call-to-user-pending>review now</call-to-user-pending>",
            "screen line one",
            "screen line two");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, -2);

        assertEquals(-1, externalRow);
    }

    @Test
    public void returnsRowOfMostRecentTagWhenMultipleArePresent() {
        List<String> rowTexts = Arrays.asList(
            "<call-to-user-pending>older request</call-to-user-pending>",
            "intervening output",
            "<call-to-user-pending>newest request</call-to-user-pending>");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, -2);

        assertEquals(0, externalRow);
    }

    @Test
    public void findsTagThatWrapsAcrossTwoVisualRows() {
        List<String> rowTexts = Arrays.asList(
            "preceding output line",
            "blah blah blah blah <call-to",
            "-user-pending>please review</call-to-user-pending>");

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
            "<call-to-user-pending>review now</call-to-user-pending>",
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

    @Test
    public void targetsTheCandidateTagCarryingTheMessageRatherThanTheStatuslineLine() {
        List<String> rowTexts = Arrays.asList(
            "preceding output line",
            "<call-to-user-pending>please approve the rollout</call-to-user-pending>",
            "call:09:00:00 out:09:00:01 reply:08:59:00",
            "<call-to-user>2026-07-28T09:00:01.234Z</call-to-user>");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, 0);

        assertEquals(1, externalRow);
    }

    @Test
    public void ignoresTheStatuslineLineWhenNoCandidateTagIsPresent() {
        List<String> rowTexts = Arrays.asList(
            "preceding output line",
            "<call-to-user>2026-07-28T09:00:01.234Z</call-to-user>");

        int externalRow = CallToUserTagScrollLocator.mostRecentOpenTagExternalRow(rowTexts, 0);

        assertEquals(CallToUserTagScrollLocator.NO_TAG_ROW, externalRow);
    }
}
