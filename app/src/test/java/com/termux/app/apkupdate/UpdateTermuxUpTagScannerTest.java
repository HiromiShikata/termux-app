package com.termux.app.apkupdate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class UpdateTermuxUpTagScannerTest {

    @Test
    public void extractsReasonOfCompleteBlock() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "before <update-termux-up>security fix</update-termux-up> after");
        assertEquals(1, reasons.size());
        assertEquals("security fix", reasons.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "<update-termux-up>\n  new build available  \n</update-termux-up>");
        assertEquals(1, reasons.size());
        assertEquals("new build available", reasons.get(0));
    }

    @Test
    public void preservesJapaneseReason() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "<update-termux-up>セキュリティ修正のため更新してください</update-termux-up>");
        assertEquals(1, reasons.size());
        assertEquals("セキュリティ修正のため更新してください", reasons.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "<update-termux-up>incomplete");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "<update-termux-up>first</update-termux-up> mid <update-termux-up>second</update-termux-up>");
        assertEquals(2, reasons.size());
        assertEquals("first", reasons.get(0));
        assertEquals("second", reasons.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons("<update-termux-up>   </update-termux-up>");
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void normalizeReturnsNullForBlankAndNull() {
        assertNull(UpdateTermuxUpTagScanner.normalizeReason(null));
        assertNull(UpdateTermuxUpTagScanner.normalizeReason("   "));
        assertEquals("done", UpdateTermuxUpTagScanner.normalizeReason("  done  "));
    }

    @Test
    public void newReasonReturnsLatestBlock() {
        UpdateTermuxUpTagScanner scanner = new UpdateTermuxUpTagScanner();
        assertEquals("second",
            scanner.newReason("<update-termux-up>first</update-termux-up><update-termux-up>second</update-termux-up>"));
    }

    @Test
    public void deduplicatesAlreadyTriggeredReasonOnRedraw() {
        UpdateTermuxUpTagScanner scanner = new UpdateTermuxUpTagScanner();
        String output = "prompt <update-termux-up>update now</update-termux-up> prompt";

        String firstScan = scanner.newReason(output);
        assertEquals("update now", firstScan);
        scanner.markTriggered(firstScan);

        assertNull(scanner.newReason(output));
    }

    @Test
    public void triggersNextNewReasonAfterPreviousTriggered() {
        UpdateTermuxUpTagScanner scanner = new UpdateTermuxUpTagScanner();

        String firstScan = scanner.newReason("<update-termux-up>first</update-termux-up>");
        assertEquals("first", firstScan);
        scanner.markTriggered(firstScan);

        String secondScan = scanner.newReason(
            "<update-termux-up>first</update-termux-up><update-termux-up>second</update-termux-up>");
        assertEquals("second", secondScan);
        scanner.markTriggered(secondScan);

        assertNull(scanner.newReason(
            "<update-termux-up>first</update-termux-up><update-termux-up>second</update-termux-up>"));
    }

    @Test
    public void returnsNullWhenNoBlockPresent() {
        UpdateTermuxUpTagScanner scanner = new UpdateTermuxUpTagScanner();
        assertNull(scanner.newReason("plain terminal output"));
        assertNull(scanner.newReason(null));
    }

    @Test
    public void doesNotMatchPartialTagName() {
        List<String> reasons = UpdateTermuxUpTagScanner.extractReasons(
            "<update-termux>x</update-termux><update-termux-update>y</update-termux-update>");
        assertTrue(reasons.isEmpty());
    }
}
