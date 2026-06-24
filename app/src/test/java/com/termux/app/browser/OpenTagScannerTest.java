package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class OpenTagScannerTest {

    @Test
    public void extractsUrlOfCompleteBlock() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("before <open>https://example.com</open> after");
        assertEquals(1, openUrls.size());
        assertEquals("https://example.com", openUrls.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("<open>\n  https://example.com/path  \n</open>");
        assertEquals(1, openUrls.size());
        assertEquals("https://example.com/path", openUrls.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("<open>https://example.com");
        assertTrue(openUrls.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("<open>https://first.example</open> mid <open>https://second.example</open>");
        assertEquals(2, openUrls.size());
        assertEquals("https://first.example", openUrls.get(0));
        assertEquals("https://second.example", openUrls.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("<open>   </open>");
        assertTrue(openUrls.isEmpty());
    }

    @Test
    public void ignoresNonHttpUrl() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls("<open>ftp://example.com</open><open>javascript:alert(1)</open><open>example.com</open>");
        assertTrue(openUrls.isEmpty());
    }

    @Test
    public void acceptsHttpAndHttpsUrls() {
        assertEquals("http://example.com", OpenTagScanner.normalizeUrl("http://example.com"));
        assertEquals("https://example.com", OpenTagScanner.normalizeUrl("https://example.com"));
    }

    @Test
    public void newOpenUrlsReturnsEachUrlInOrderOnFirstScan() {
        OpenTagScanner scanner = new OpenTagScanner();
        List<String> openUrls = scanner.newOpenUrls(
            "<open>https://first.example</open><open>https://second.example</open>");
        assertEquals(2, openUrls.size());
        assertEquals("https://first.example", openUrls.get(0));
        assertEquals("https://second.example", openUrls.get(1));
    }

    @Test
    public void deduplicatesAlreadyOpenedUrlOnRedraw() {
        OpenTagScanner scanner = new OpenTagScanner();
        String output = "prompt <open>https://example.com</open> prompt";

        assertEquals(1, scanner.newOpenUrls(output).size());
        assertTrue(scanner.newOpenUrls(output).isEmpty());
    }

    @Test
    public void opensNextNewUrlAfterPreviousOpened() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://first.example"),
            scanner.newOpenUrls("<open>https://first.example</open>"));
        assertEquals(List.of("https://second.example"),
            scanner.newOpenUrls("<open>https://first.example</open><open>https://second.example</open>"));
        assertTrue(scanner.newOpenUrls(
            "<open>https://first.example</open><open>https://second.example</open>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertTrue(scanner.newOpenUrls("plain terminal output").isEmpty());
        assertTrue(scanner.newOpenUrls(null).isEmpty());
    }

    @Test
    public void doesNotReOpenAnAlreadyOpenedUrlThatRemainsInScrollbackWhenMoreOutputArrives() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.newOpenUrls("<open>https://example.com/a</open>"));

        assertTrue(scanner.newOpenUrls(
            "<open>https://example.com/a</open>\nmore output line 1").isEmpty());
        assertTrue(scanner.newOpenUrls(
            "<open>https://example.com/a</open>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void firesTwoNewUrlsAppearingInASingleUpdateEachExactlyOnce() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.newOpenUrls("<open>https://example.com/a</open>"));

        List<String> burst = scanner.newOpenUrls(
            "<open>https://example.com/a</open><open>https://example.com/b</open><open>https://example.com/c</open>");
        assertEquals(2, burst.size());
        assertEquals("https://example.com/b", burst.get(0));
        assertEquals("https://example.com/c", burst.get(1));
    }

    @Test
    public void opensAGenuinelyNewUrlAfterEarlierUrlsHaveBeenTrimmedOutOfTheTranscript() {
        OpenTagScanner scanner = new OpenTagScanner();

        StringBuilder longTranscript = new StringBuilder("<open>https://example.com/a</open>\n");
        for (int line = 0; line < 5000; line++) {
            longTranscript.append("output line ").append(line).append('\n');
        }
        assertEquals(List.of("https://example.com/a"), scanner.newOpenUrls(longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<open>https://example.com/b</open>\n";
        assertEquals(List.of("https://example.com/b"), scanner.newOpenUrls(trimmedWithNewTag));
    }
}
