package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class CallToUserTagScannerTest {

    @Test
    public void extractsReasonOfCompleteBlock() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "before <call-to-user>needs approval</call-to-user> after");
        assertEquals(1, reasons.size());
        assertEquals("needs approval", reasons.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "<call-to-user>\n  please review the diff  \n</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("please review the diff", reasons.get(0));
    }

    @Test
    public void preservesJapaneseReason() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "<call-to-user>承認をお願いします</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("承認をお願いします", reasons.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> reasons = CallToUserTagScanner.extractReasons("<call-to-user>incomplete");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "<call-to-user>first</call-to-user> mid <call-to-user>second</call-to-user>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> reasons = CallToUserTagScanner.extractReasons("<call-to-user>   </call-to-user>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void normalizeReturnsNullForBlankAndNull() {
        assertNull(CallToUserTagScanner.normalizeReason(null));
        assertNull(CallToUserTagScanner.normalizeReason("   "));
        assertEquals("done", CallToUserTagScanner.normalizeReason("  done  "));
    }

    @Test
    public void newReasonReturnsLatestBlock() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        assertEquals("second",
            scanner.newReason("<call-to-user>first</call-to-user><call-to-user>second</call-to-user>"));
    }

    @Test
    public void deduplicatesAlreadyTriggeredReasonOnRedraw() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user>needs approval</call-to-user> prompt";

        String firstScan = scanner.newReason(output);
        assertEquals("needs approval", firstScan);
        scanner.markTriggered(firstScan);

        assertNull(scanner.newReason(output));
    }

    @Test
    public void triggersNextNewReasonAfterPreviousTriggered() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        String firstScan = scanner.newReason("<call-to-user>first</call-to-user>");
        assertEquals("first", firstScan);
        scanner.markTriggered(firstScan);

        String secondScan = scanner.newReason(
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>");
        assertEquals("second", secondScan);
        scanner.markTriggered(secondScan);

        assertNull(scanner.newReason(
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>"));
    }

    @Test
    public void returnsNullWhenNoBlockPresent() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        assertNull(scanner.newReason("plain terminal output"));
        assertNull(scanner.newReason(null));
    }

    @Test
    public void doesNotMatchPartialTagName() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "<call-to>x</call-to><call-to-user-extra>y</call-to-user-extra>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void doesNotReFireWhenAnAlreadyFiredTagRemainsInScrollbackAndMoreOutputArrives() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals("approval", scanner.newReason("<call-to-user>approval</call-to-user>"));

        assertNull(scanner.newReason("<call-to-user>approval</call-to-user>\nmore output line 1"));
        assertNull(scanner.newReason(
            "<call-to-user>approval</call-to-user>\nmore output line 1\nmore output line 2"));
    }

    @Test
    public void doesNotReFireEarlierTagWhenALaterTagScrollsOutOfTheTranscript() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals("alpha", scanner.newReason("<call-to-user>alpha</call-to-user>"));
        assertEquals("beta", scanner.newReason(
            "<call-to-user>alpha</call-to-user><call-to-user>beta</call-to-user>"));

        assertNull(scanner.newReason("<call-to-user>alpha</call-to-user>"));
    }

    @Test
    public void firesAGenuinelyNewTagAfterAnEarlierTagScrolledOut() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals("alpha", scanner.newReason("<call-to-user>alpha</call-to-user>"));
        assertEquals("beta", scanner.newReason(
            "<call-to-user>alpha</call-to-user><call-to-user>beta</call-to-user>"));
        assertNull(scanner.newReason("<call-to-user>alpha</call-to-user>"));

        assertEquals("gamma", scanner.newReason(
            "<call-to-user>alpha</call-to-user><call-to-user>gamma</call-to-user>"));
    }
}
