package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.termux.app.outputtag.OutputTagOccurrence;

import java.util.ArrayList;
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
    public void extractsAnEmptyReasonFromABlockThatCarriesNoBody() {
        List<String> reasons = CallToUserTagScanner.extractReasons("<call-to-user>   </call-to-user>");
        assertEquals(List.of(""), reasons);
    }

    @Test
    public void normalizeReturnsNullOnlyForNullAndAnEmptyStringForBlank() {
        assertNull(CallToUserTagScanner.normalizeReason(null));
        assertEquals("", CallToUserTagScanner.normalizeReason("   "));
        assertEquals("done", CallToUserTagScanner.normalizeReason("  done  "));
    }

    @Test
    public void preservesRunsOfHorizontalWhitespaceWhenTrimmingOnly() {
        assertEquals("a    b\nc  d", CallToUserTagScanner.normalizeReason("a    b\nc  d"));
    }

    @Test
    public void preservesTabsAndMixedHorizontalWhitespace() {
        assertEquals("a \t \t b\nc\t\td",
            CallToUserTagScanner.normalizeReason("a \t \t b\nc\t\td"));
    }

    @Test
    public void extractedReasonPreservesInternalWhitespaceRaw() {
        List<String> reasons = CallToUserTagScanner.extractReasons(
            "<call-to-user>please    review\nthe    diff</call-to-user>");
        assertEquals(1, reasons.size());
        assertEquals("please    review\nthe    diff", reasons.get(0));
    }

    @Test
    public void newReasonsReturnsEachReasonInOrderOnFirstScan() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        List<String> reasons = newReasons(scanner, 
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void deduplicatesAlreadyFiredReasonOnRedraw() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user>needs approval</call-to-user> prompt";

        assertEquals(1, newReasons(scanner, output).size());
        assertTrue(newReasons(scanner, output).isEmpty());
    }

    @Test
    public void firesNextNewReasonAfterPreviousFired() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("first"),
            newReasons(scanner, "<call-to-user>first</call-to-user>"));
        assertEquals(List.of("second"),
            newReasons(scanner, "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>"));
        assertTrue(newReasons(scanner, 
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        assertTrue(newReasons(scanner, "plain terminal output").isEmpty());
        assertTrue(newReasons(scanner, null).isEmpty());
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

        assertEquals(List.of("approval"),
            newReasons(scanner, "<call-to-user>approval</call-to-user>"));

        assertTrue(newReasons(scanner, 
            "<call-to-user>approval</call-to-user>\nmore output line 1").isEmpty());
        assertTrue(newReasons(scanner, 
            "<call-to-user>approval</call-to-user>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void firesTwoNewTagsAppearingInASingleUpdateEachExactlyOnce() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("alpha"),
            newReasons(scanner, "<call-to-user>alpha</call-to-user>"));

        List<String> burst = newReasons(scanner, 
            "<call-to-user>alpha</call-to-user><call-to-user>beta</call-to-user><call-to-user>gamma</call-to-user>");
        assertEquals(2, burst.size());
        assertEquals("beta", burst.get(0));
        assertEquals("gamma", burst.get(1));
    }

    @Test
    public void firesAGenuinelyNewTagAfterEarlierTagsHaveBeenTrimmedOutOfTheTranscript() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        StringBuilder longTranscript = new StringBuilder("<call-to-user>approval</call-to-user>\n");
        for (int line = 0; line < 5000; line++) {
            longTranscript.append("output line ").append(line).append('\n');
        }
        assertEquals(List.of("approval"), newReasons(scanner, longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<call-to-user>review now</call-to-user>\n";
        assertEquals(List.of("review now"), newReasons(scanner, trimmedWithNewTag));
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagWithInternalWhitespaceOnReScan() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();
        String output = "prompt <call-to-user>needs    review of   diff</call-to-user> tail";

        assertEquals(List.of("needs    review of   diff"), newReasons(scanner, output));
        assertTrue(newReasons(scanner, output).isEmpty());
        assertTrue(newReasons(scanner, 
            output + "\nmore output appended after the answer").isEmpty());
    }

    @Test
    public void doesNotReFireAnAlreadyFiredTagWhenLaterTranscriptHasTrimmedTheTagAway() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("approval"),
            newReasons(scanner, "<call-to-user>approval</call-to-user>\nline a\nline b\n"));
        assertTrue(newReasons(scanner, 
            "line a\nline b\nstill the same already fired output\n").isEmpty());
    }

    @Test
    public void doesNotReFireAnEarlierReasonWhenAReFedWindowShowsItWhileTheNewerReasonScrolledOffTheTail() {
        CallToUserTagScanner scanner = new CallToUserTagScanner();

        assertEquals(List.of("first"),
            newReasons(scanner, "<call-to-user>first</call-to-user>\n"));
        assertEquals(List.of("second"),
            newReasons(scanner, "<call-to-user>first</call-to-user>\n<call-to-user>second</call-to-user>\n"));

        assertTrue(newReasons(scanner, 
            "<call-to-user>first</call-to-user>\nlater plain output\n").isEmpty());

        assertEquals(List.of("third"),
            newReasons(scanner, "<call-to-user>first</call-to-user>\nlater plain output\n<call-to-user>third</call-to-user>\n"));
    }

    private static List<String> newReasons(CallToUserTagScanner scanner, String output) {
        List<String> reasons = new ArrayList<>();
        for (OutputTagOccurrence call : scanner.newCalls(output)) {
            reasons.add(call.getValue());
        }
        return reasons;
    }
}
