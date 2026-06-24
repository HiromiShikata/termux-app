package com.termux.app.outputtag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class OutputTagScannerTest {

    private static OutputTagScanner trimNormalizingScanner() {
        return new OutputTagScanner("tag", OutputTagScannerTest::trimToNull);
    }

    private static String trimToNull(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Test
    public void firstScanReturnsEveryCompleteOccurrenceInOrder() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a", "b"),
            scanner.newValues("<tag>a</tag> mid <tag>b</tag>"));
    }

    @Test
    public void reScanningTheExactSameTranscriptDoesNotReFire() {
        OutputTagScanner scanner = trimNormalizingScanner();
        String transcript = "<tag>a</tag><tag>b</tag>";
        assertEquals(List.of("a", "b"), scanner.newValues(transcript));
        assertTrue(scanner.newValues(transcript).isEmpty());
    }

    @Test
    public void appendingPlainOutputAfterAFiredTagDoesNotReFireThatTag() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        assertTrue(scanner.newValues("<tag>a</tag>\nline 1").isEmpty());
        assertTrue(scanner.newValues("<tag>a</tag>\nline 1\nline 2").isEmpty());
    }

    @Test
    public void firesEachOfTwoNewTagsAppearingInOneUpdateExactlyOnce() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        assertEquals(List.of("b", "c"),
            scanner.newValues("<tag>a</tag><tag>b</tag><tag>c</tag>"));
    }

    @Test
    public void firesATagWhoseOpeningArrivedBeforeAndClosingArrivesAfterTheBoundary() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertTrue(scanner.newValues("prefix output <tag>partial").isEmpty());
        assertEquals(List.of("partial closed now"),
            scanner.newValues("prefix output <tag>partial closed now</tag>"));
    }

    @Test
    public void firesANewTagAfterEarlierContentScrolledOutOfTheTranscript() {
        OutputTagScanner scanner = trimNormalizingScanner();

        StringBuilder transcript = new StringBuilder("<tag>first</tag>\n");
        for (int line = 0; line < 4000; line++) {
            transcript.append("line ").append(line).append('\n');
        }
        assertEquals(List.of("first"), scanner.newValues(transcript.toString()));

        String trimmed = "line 3998\nline 3999\n<tag>second</tag>\n";
        assertEquals(List.of("second"), scanner.newValues(trimmed));
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagAfterTheTranscriptTrimmedItAway() {
        OutputTagScanner scanner = trimNormalizingScanner();

        assertEquals(List.of("first"),
            scanner.newValues("<tag>first</tag>\nline a\nline b\n"));
        assertTrue(scanner.newValues("line a\nline b\nplain continuation\n").isEmpty());
    }

    @Test
    public void treatsNullTranscriptAsResettingProcessedState() {
        OutputTagScanner scanner = trimNormalizingScanner();
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
        assertTrue(scanner.newValues(null).isEmpty());
        assertEquals(List.of("a"), scanner.newValues("<tag>a</tag>"));
    }
}
