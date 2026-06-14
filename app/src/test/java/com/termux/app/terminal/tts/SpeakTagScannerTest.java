package com.termux.app.terminal.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SpeakTagScannerTest {

    @Test
    public void extractsInnerTextOfCompleteBlock() {
        List<String> spokenTexts = SpeakTagScanner.extractSpokenTexts("before <speak>こんにちは</speak> after");
        assertEquals(1, spokenTexts.size());
        assertEquals("こんにちは", spokenTexts.get(0));
    }

    @Test
    public void extractsAcrossNewlinesWithDotallSemantics() {
        List<String> spokenTexts = SpeakTagScanner.extractSpokenTexts("<speak>line one\nline two</speak>");
        assertEquals(1, spokenTexts.size());
        assertEquals("line one line two", spokenTexts.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> spokenTexts = SpeakTagScanner.extractSpokenTexts("<speak>still streaming");
        assertTrue(spokenTexts.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> spokenTexts = SpeakTagScanner.extractSpokenTexts("<speak>first</speak> middle <speak>second</speak>");
        assertEquals(2, spokenTexts.size());
        assertEquals("first", spokenTexts.get(0));
        assertEquals("second", spokenTexts.get(1));
    }

    @Test
    public void stripsSurroundingMarkdownDecoration() {
        assertEquals("bold summary", SpeakTagScanner.stripDecoration("**bold** `summary`"));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> spokenTexts = SpeakTagScanner.extractSpokenTexts("<speak>   </speak>");
        assertTrue(spokenTexts.isEmpty());
    }

    @Test
    public void newSpokenTextReturnsLatestBlock() {
        SpeakTagScanner scanner = new SpeakTagScanner();
        assertEquals("second", scanner.newSpokenText("<speak>first</speak><speak>second</speak>"));
    }

    @Test
    public void deduplicatesAlreadySpokenBlockOnRedraw() {
        SpeakTagScanner scanner = new SpeakTagScanner();
        String output = "prompt <speak>summary</speak> prompt";

        String firstScan = scanner.newSpokenText(output);
        assertEquals("summary", firstScan);
        scanner.markSpoken(firstScan);

        assertNull(scanner.newSpokenText(output));
    }

    @Test
    public void speaksNextNewBlockAfterPreviousSpoken() {
        SpeakTagScanner scanner = new SpeakTagScanner();

        String firstScan = scanner.newSpokenText("<speak>first</speak>");
        assertEquals("first", firstScan);
        scanner.markSpoken(firstScan);

        String secondScan = scanner.newSpokenText("<speak>first</speak><speak>second</speak>");
        assertEquals("second", secondScan);
        scanner.markSpoken(secondScan);

        assertNull(scanner.newSpokenText("<speak>first</speak><speak>second</speak>"));
    }

    @Test
    public void returnsNullWhenNoBlockPresent() {
        SpeakTagScanner scanner = new SpeakTagScanner();
        assertNull(scanner.newSpokenText("plain terminal output"));
        assertNull(scanner.newSpokenText(null));
    }
}
