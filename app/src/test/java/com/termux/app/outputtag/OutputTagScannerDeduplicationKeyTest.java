package com.termux.app.outputtag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OutputTagScannerDeduplicationKeyTest {

    private static final String CONSTANT_VALUE = "marker";

    private static String trimToNull(String innerText) {
        if (innerText == null) return null;
        String trimmed = innerText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static OutputTagScanner scannerKeyedOnPrecedingCycleLine() {
        return new OutputTagScanner("tag", OutputTagScannerDeduplicationKeyTest::trimToNull,
            (value, output, occurrenceStartIndex) -> {
                String precedingText = output.substring(0, occurrenceStartIndex);
                int cycleMarkerIndex = precedingText.lastIndexOf("cycle:");
                if (cycleMarkerIndex < 0) {
                    return value;
                }
                return precedingText.substring(cycleMarkerIndex,
                    cycleMarkerIndex + "cycle:0".length()) + " " + value;
            });
    }

    private static List<String> keysOf(List<OutputTagOccurrence> occurrences) {
        List<String> keys = new ArrayList<>();
        for (OutputTagOccurrence occurrence : occurrences) {
            keys.add(occurrence.getDeduplicationKey());
        }
        return keys;
    }

    @Test
    public void aConstantValueUnderTheDefaultKeyStopsFiringOnceTheEarlierOccurrenceScrolledOff() {
        OutputTagScanner scanner = new OutputTagScanner("tag",
            OutputTagScannerDeduplicationKeyTest::trimToNull);

        assertEquals(List.of(CONSTANT_VALUE),
            scanner.newValues("cycle:1\n<tag>" + CONSTANT_VALUE + "</tag>\n"));

        assertTrue(scanner.newValues("cycle:2\n<tag>" + CONSTANT_VALUE + "</tag>\n").isEmpty());
    }

    @Test
    public void aConstantValueKeyedOnPerOccurrenceContextFiresForEveryNewCycle() {
        OutputTagScanner scanner = scannerKeyedOnPrecedingCycleLine();

        assertEquals(List.of(CONSTANT_VALUE),
            scanner.newValues("cycle:1\n<tag>" + CONSTANT_VALUE + "</tag>\n"));
        assertEquals(List.of(CONSTANT_VALUE),
            scanner.newValues("cycle:2\n<tag>" + CONSTANT_VALUE + "</tag>\n"));
        assertEquals(List.of(CONSTANT_VALUE),
            scanner.newValues("cycle:3\n<tag>" + CONSTANT_VALUE + "</tag>\n"));
    }

    @Test
    public void reScanningAnUnchangedWindowDoesNotReFireAKeyedOccurrence() {
        OutputTagScanner scanner = scannerKeyedOnPrecedingCycleLine();
        String window = "cycle:1\n<tag>" + CONSTANT_VALUE + "</tag>\n";

        assertEquals(List.of(CONSTANT_VALUE), scanner.newValues(window));
        assertTrue(scanner.newValues(window).isEmpty());
        assertTrue(scanner.newValues(window + "later plain output\n").isEmpty());
    }

    @Test
    public void oneOutputCarryingTheSameOccurrenceTwiceReportsBothUnderOneSharedKey() {
        OutputTagScanner scanner = scannerKeyedOnPrecedingCycleLine();

        List<OutputTagOccurrence> occurrences = scanner.newOccurrences("cycle:1\n<tag>"
            + CONSTANT_VALUE + "</tag>\n<tag>" + CONSTANT_VALUE + "</tag>\n");

        assertEquals(2, occurrences.size());
        assertEquals(List.of("cycle:1 " + CONSTANT_VALUE, "cycle:1 " + CONSTANT_VALUE),
            keysOf(occurrences));
    }

    @Test
    public void newOccurrencesReportsTheResolvedKeyAlongsideTheValue() {
        OutputTagScanner scanner = scannerKeyedOnPrecedingCycleLine();

        List<OutputTagOccurrence> occurrences =
            scanner.newOccurrences("cycle:7\n<tag>" + CONSTANT_VALUE + "</tag>\n");

        assertEquals(List.of("cycle:7 " + CONSTANT_VALUE), keysOf(occurrences));
        assertEquals(List.of(CONSTANT_VALUE), List.of(occurrences.get(0).getValue()));
    }

    @Test
    public void aNullTranscriptReportsNoOccurrences() {
        assertTrue(scannerKeyedOnPrecedingCycleLine().newOccurrences(null).isEmpty());
    }
}
