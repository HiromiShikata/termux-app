package com.termux.app.apkupdate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class UpdateTermuxAppTagScannerTest {

    @Test
    public void extractsReasonOfCompleteBlock() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "before <update-termux-app>security fix</update-termux-app> after");
        assertEquals(1, reasons.size());
        assertEquals("security fix", reasons.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux-app>\n  new build available  \n</update-termux-app>");
        assertEquals(1, reasons.size());
        assertEquals("new build available", reasons.get(0));
    }

    @Test
    public void preservesJapaneseReason() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux-app>セキュリティ修正のため更新してください</update-termux-app>");
        assertEquals(1, reasons.size());
        assertEquals("セキュリティ修正のため更新してください", reasons.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux-app>incomplete");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux-app>first</update-termux-app> mid <update-termux-app>second</update-termux-app>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons("<update-termux-app>   </update-termux-app>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void normalizeReturnsNullForBlankAndNull() {
        assertNull(UpdateTermuxAppTagScanner.normalizeReason(null));
        assertNull(UpdateTermuxAppTagScanner.normalizeReason("   "));
        assertEquals("done", UpdateTermuxAppTagScanner.normalizeReason("  done  "));
    }

    @Test
    public void newReasonsReturnsEachReasonInOrderOnFirstScan() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        List<String> reasons = scanner.newReasons(
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void deduplicatesAlreadyTriggeredReasonOnRedraw() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        String output = "prompt <update-termux-app>update now</update-termux-app> prompt";

        assertEquals(1, scanner.newReasons(output).size());
        assertTrue(scanner.newReasons(output).isEmpty());
    }

    @Test
    public void triggersNextNewReasonAfterPreviousTriggered() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();

        assertEquals(List.of("first"),
            scanner.newReasons("<update-termux-app>first</update-termux-app>"));
        assertEquals(List.of("second"),
            scanner.newReasons("<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>"));
        assertTrue(scanner.newReasons(
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        assertTrue(scanner.newReasons("plain terminal output").isEmpty());
        assertTrue(scanner.newReasons(null).isEmpty());
    }

    @Test
    public void doesNotMatchPartialTagName() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux>x</update-termux><update-termux-update>y</update-termux-update>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void doesNotReTriggerWhenAnAlreadyTriggeredTagRemainsInScrollbackAndMoreOutputArrives() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();

        assertEquals(List.of("update now"),
            scanner.newReasons("<update-termux-app>update now</update-termux-app>"));

        assertTrue(scanner.newReasons(
            "<update-termux-app>update now</update-termux-app>\nmore output line 1").isEmpty());
        assertTrue(scanner.newReasons(
            "<update-termux-app>update now</update-termux-app>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void triggersTwoNewTagsAppearingInASingleUpdateEachExactlyOnce() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();

        assertEquals(List.of("first"),
            scanner.newReasons("<update-termux-app>first</update-termux-app>"));

        List<String> burst = scanner.newReasons(
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app><update-termux-app>third</update-termux-app>");
        assertEquals(2, burst.size());
        assertEquals("second", burst.get(0));
        assertEquals("third", burst.get(1));
    }

    @Test
    public void triggersAGenuinelyNewTagAfterEarlierTagsHaveBeenTrimmedOutOfTheTranscript() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();

        StringBuilder longTranscript = new StringBuilder("<update-termux-app>update now</update-termux-app>\n");
        for (int line = 0; line < 5000; line++) {
            longTranscript.append("output line ").append(line).append('\n');
        }
        assertEquals(List.of("update now"), scanner.newReasons(longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<update-termux-app>update again</update-termux-app>\n";
        assertEquals(List.of("update again"), scanner.newReasons(trimmedWithNewTag));
    }
}
