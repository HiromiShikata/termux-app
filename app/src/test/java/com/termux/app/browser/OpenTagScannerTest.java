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
    public void ignoresBlockWhoseUrlContainsJumpToBottomHint() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls(
            "<open>https://github.com/xcare-medica Jump to bottom (ctrl+End) ↓ 81</open>");
        assertTrue(openUrls.isEmpty());
    }

    @Test
    public void ignoresBlockWhoseUrlContainsPercentEncodedJumpToBottomHint() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls(
            "<open>https://github.com/xcare-medica%20Jump%20to%20bottom%20(ctrl+End)%20%E2%86%93%2081</open>");
        assertTrue(openUrls.isEmpty());
    }

    @Test
    public void urlsToOpenReturnsEachUrlInOrderOnFirstScan() {
        OpenTagScanner scanner = new OpenTagScanner();
        List<String> openUrls = scanner.urlsToOpen(
            "<open>https://first.example</open><open>https://second.example</open>");
        assertEquals(2, openUrls.size());
        assertEquals("https://first.example", openUrls.get(0));
        assertEquals("https://second.example", openUrls.get(1));
    }

    @Test
    public void deduplicatesAlreadyOpenedUrlOnRedraw() {
        OpenTagScanner scanner = new OpenTagScanner();
        String output = "prompt <open>https://example.com</open> prompt";

        assertEquals(1, scanner.urlsToOpen(output).size());
        assertTrue(scanner.urlsToOpen(output).isEmpty());
    }

    @Test
    public void opensNextNewUrlAfterPreviousOpened() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://first.example"),
            scanner.urlsToOpen("<open>https://first.example</open>"));
        assertEquals(List.of("https://second.example"),
            scanner.urlsToOpen("<open>https://first.example</open><open>https://second.example</open>"));
        assertTrue(scanner.urlsToOpen(
            "<open>https://first.example</open><open>https://second.example</open>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertTrue(scanner.urlsToOpen("plain terminal output").isEmpty());
        assertTrue(scanner.urlsToOpen(null).isEmpty());
    }

    @Test
    public void opensABareUrlThatIsTheSoleContentOfItsOwnLine() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertEquals(List.of("https://example.com/bare"),
            scanner.urlsToOpen("running\nhttps://example.com/bare\ndone\n"));
    }

    @Test
    public void doesNotOpenABareUrlEmbeddedInASentenceOrLogLine() {
        OpenTagScanner scanner = new OpenTagScanner();
        assertTrue(scanner.urlsToOpen("Open https://example.com/page now\n").isEmpty());
        assertTrue(scanner.urlsToOpen("INFO fetched https://example.com/api ok\n").isEmpty());
    }

    @Test
    public void deduplicatesAlreadyOpenedBareUrlOnRedraw() {
        OpenTagScanner scanner = new OpenTagScanner();
        String output = "task\nhttps://example.com/bare\n";

        assertEquals(1, scanner.urlsToOpen(output).size());
        assertTrue(scanner.urlsToOpen(output).isEmpty());
    }

    @Test
    public void stillDetectsTaggedUrlsAlongsideBareUrlDetection() {
        OpenTagScanner scanner = new OpenTagScanner();
        List<String> opened = scanner.urlsToOpen(
            "<open>https://example.com/tagged</open>\nhttps://example.com/bare\n");

        assertEquals(2, opened.size());
        assertEquals("https://example.com/tagged", opened.get(0));
        assertEquals("https://example.com/bare", opened.get(1));
    }

    @Test
    public void doesNotOpenTheSameUrlTwiceWhenItAppearsBothTaggedAndBare() {
        OpenTagScanner scanner = new OpenTagScanner();
        List<String> opened = scanner.urlsToOpen(
            "<open>https://example.com/dup</open>\nhttps://example.com/dup\n");

        assertEquals(List.of("https://example.com/dup"), opened);
    }

    @Test
    public void doesNotReOpenAnAlreadyOpenedUrlThatRemainsInScrollbackWhenMoreOutputArrives() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.urlsToOpen("<open>https://example.com/a</open>"));

        assertTrue(scanner.urlsToOpen(
            "<open>https://example.com/a</open>\nmore output line 1").isEmpty());
        assertTrue(scanner.urlsToOpen(
            "<open>https://example.com/a</open>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void firesTwoNewUrlsAppearingInASingleUpdateEachExactlyOnce() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.urlsToOpen("<open>https://example.com/a</open>"));

        List<String> burst = scanner.urlsToOpen(
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
        assertEquals(List.of("https://example.com/a"), scanner.urlsToOpen(longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<open>https://example.com/b</open>\n";
        assertEquals(List.of("https://example.com/b"), scanner.urlsToOpen(trimmedWithNewTag));
    }

    @Test
    public void doesNotReOpenAnEarlierOpenedUrlWhenALaterUrlScrollsOutAndTheEarlierUrlReappearsAtTheWindowFront() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.urlsToOpen("header\n<open>https://example.com/a</open>\n"));
        assertEquals(List.of("https://example.com/b"),
            scanner.urlsToOpen(
                "<open>https://example.com/a</open>\nmid\n<open>https://example.com/b</open>\n"));

        assertTrue(scanner.urlsToOpen(
            "old\n<open>https://example.com/a</open>\nold2\n").isEmpty());
    }

    @Test
    public void doesNotReOpenAnAlreadyOpenedUrlEvenWhenTheSameUrlIsEmittedAgainAsAFreshOccurrence() {
        OpenTagScanner scanner = new OpenTagScanner();

        assertEquals(List.of("https://example.com/a"),
            scanner.urlsToOpen("<open>https://example.com/a</open>\nline\n"));
        assertTrue(scanner.urlsToOpen(
            "<open>https://example.com/a</open>\nline\n<open>https://example.com/a</open>\n").isEmpty());
    }

    @Test
    public void taggedUrlWithAdjacentFullWidthTextIsTruncatedBeforeTheFullWidthCharacters() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls(
            "<open>https://example.com/owner/repo/pull/123（説明文）</open>");
        assertEquals(List.of("https://example.com/owner/repo/pull/123"), openUrls);
    }

    @Test
    public void taggedUrlFollowedByNewlineThenFullWidthTextYieldsOnlyTheUrl() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls(
            "<open>https://example.com/owner/repo/pull/123\n（説明文）</open>");
        assertEquals(List.of("https://example.com/owner/repo/pull/123"), openUrls);
    }

    @Test
    public void taggedUrlWithAsciiQueryAndFragmentIsPreserved() {
        List<String> openUrls = OpenTagScanner.extractOpenUrls(
            "<open>https://example.com/search?q=hello+world&x=1#section</open>");
        assertEquals(List.of("https://example.com/search?q=hello+world&x=1#section"), openUrls);
    }
}
