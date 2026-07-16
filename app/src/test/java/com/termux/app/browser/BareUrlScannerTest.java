package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class BareUrlScannerTest {

    @Test
    public void detectsUrlAloneOnItsOwnLine() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertEquals(List.of("https://example.com/path"),
            scanner.urlsToOpen("running task\nhttps://example.com/path\ndone\n"));
    }

    @Test
    public void detectsLineSoleUrlEvenWhenSurroundedBySpaces() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertEquals(List.of("https://example.com/spaced"),
            scanner.urlsToOpen("   https://example.com/spaced   \n"));
    }

    @Test
    public void detectsHttpAndHttpsLineSoleUrls() {
        assertEquals(List.of("http://example.com"),
            BareUrlScanner.extractLineSoleUrls("http://example.com\n"));
        assertEquals(List.of("https://example.com"),
            BareUrlScanner.extractLineSoleUrls("https://example.com\n"));
    }

    @Test
    public void doesNotDetectUrlEmbeddedInASentence() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen("Open https://example.com/page to continue\n").isEmpty());
    }

    @Test
    public void doesNotDetectUrlEmbeddedInALogLine() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen(
            "2026-06-26 12:00:00 INFO fetched https://example.com/api ok\n").isEmpty());
    }

    @Test
    public void doesNotDetectUrlFollowedByOtherTextOnTheSameLine() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen("https://example.com/page (click me)\n").isEmpty());
    }

    @Test
    public void doesNotDetectNonHttpLineSoleToken() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen("ftp://example.com\nexample.com\n/local/path\n").isEmpty());
    }

    @Test
    public void doesNotReturnTheSameUrlTwiceForTheSameSessionAcrossScreenUpdates() {
        BareUrlScanner scanner = new BareUrlScanner();
        String firstUpdate = "task\nhttps://example.com/once\n";

        assertEquals(List.of("https://example.com/once"), scanner.urlsToOpen(firstUpdate));
        assertTrue(scanner.urlsToOpen(firstUpdate).isEmpty());
        assertTrue(scanner.urlsToOpen(firstUpdate + "more output\n").isEmpty());
    }

    @Test
    public void opensANewlyAppearingLineSoleUrlAfterAnEarlierOneWasAlreadyOpened() {
        BareUrlScanner scanner = new BareUrlScanner();

        assertEquals(List.of("https://example.com/first"),
            scanner.urlsToOpen("https://example.com/first\n"));
        assertEquals(List.of("https://example.com/second"),
            scanner.urlsToOpen("https://example.com/first\nlog\nhttps://example.com/second\n"));
    }

    @Test
    public void doesNotOpenLineSoleUrlContainingPercentEncodedJumpToBottomHint() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen(
            "https://github.com/xcare-medica%20Jump%20to%20bottom%20(ctrl+End)%20%E2%86%93%2081\n").isEmpty());
    }

    @Test
    public void doesNotOpenLineSoleUrlEndingWithDownArrowHint() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen(
            "https://github.com/HiromiShikata/termux-app/issues/1↓\n").isEmpty());
    }

    @Test
    public void returnsEmptyForNullOrPlainOutput() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertTrue(scanner.urlsToOpen(null).isEmpty());
        assertTrue(scanner.urlsToOpen("plain terminal output with no url\n").isEmpty());
    }

    @Test
    public void truncatesUrlBeforeAdjacentFullWidthTextJoinedBySoftWrap() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertEquals(List.of("https://example.com/owner/repo/pull/123"),
            scanner.urlsToOpen("https://example.com/owner/repo/pull/123（説明文がここに続く）\n"));
    }

    @Test
    public void detectsUrlWhenFollowingFullWidthTextIsOnASeparateLine() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertEquals(List.of("https://example.com/owner/repo/pull/123"),
            scanner.urlsToOpen("https://example.com/owner/repo/pull/123\n（説明文がここに続く）\n"));
    }

    @Test
    public void preservesLineSoleUrlWithAsciiQueryAndFragment() {
        BareUrlScanner scanner = new BareUrlScanner();
        assertEquals(List.of("https://example.com/search?q=hello+world&x=1#section"),
            scanner.urlsToOpen("https://example.com/search?q=hello+world&x=1#section\n"));
    }
}
