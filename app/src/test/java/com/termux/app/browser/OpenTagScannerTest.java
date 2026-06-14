package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void newOpenUrlReturnsLatestBlock() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertEquals("https://second.example", scanner.newOpenUrl("<open>https://first.example</open><open>https://second.example</open>"));
    }

    @Test
    public void deduplicatesAlreadyOpenedUrlOnRedraw() {
        OpenTagScanner scanner = new OpenTagScanner();
        String output = "prompt <open>https://example.com</open> prompt";

        String firstScan = scanner.newOpenUrl(output);
        assertEquals("https://example.com", firstScan);
        scanner.markOpened(firstScan);

        assertNull(scanner.newOpenUrl(output));
    }

    @Test
    public void opensNextNewUrlAfterPreviousOpened() {
        OpenTagScanner scanner = new OpenTagScanner();

        String firstScan = scanner.newOpenUrl("<open>https://first.example</open>");
        assertEquals("https://first.example", firstScan);
        scanner.markOpened(firstScan);

        String secondScan = scanner.newOpenUrl("<open>https://first.example</open><open>https://second.example</open>");
        assertEquals("https://second.example", secondScan);
        scanner.markOpened(secondScan);

        assertNull(scanner.newOpenUrl("<open>https://first.example</open><open>https://second.example</open>"));
    }

    @Test
    public void returnsNullWhenNoBlockPresent() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertNull(scanner.newOpenUrl("plain terminal output"));
        assertNull(scanner.newOpenUrl(null));
    }
}
