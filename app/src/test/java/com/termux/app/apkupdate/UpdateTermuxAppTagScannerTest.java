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
    public void newReasonReturnsLatestBlock() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        assertEquals("second",
            scanner.newReason("<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>"));
    }

    @Test
    public void deduplicatesAlreadyTriggeredReasonOnRedraw() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        String output = "prompt <update-termux-app>update now</update-termux-app> prompt";

        String firstScan = scanner.newReason(output);
        assertEquals("update now", firstScan);
        scanner.markTriggered(firstScan);

        assertNull(scanner.newReason(output));
    }

    @Test
    public void triggersNextNewReasonAfterPreviousTriggered() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();

        String firstScan = scanner.newReason("<update-termux-app>first</update-termux-app>");
        assertEquals("first", firstScan);
        scanner.markTriggered(firstScan);

        String secondScan = scanner.newReason(
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>");
        assertEquals("second", secondScan);
        scanner.markTriggered(secondScan);

        assertNull(scanner.newReason(
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>"));
    }

    @Test
    public void returnsNullWhenNoBlockPresent() {
        UpdateTermuxAppTagScanner scanner = new UpdateTermuxAppTagScanner();
        assertNull(scanner.newReason("plain terminal output"));
        assertNull(scanner.newReason(null));
    }

    @Test
    public void doesNotMatchPartialTagName() {
        List<String> reasons = UpdateTermuxAppTagScanner.extractReasons(
            "<update-termux>x</update-termux><update-termux-update>y</update-termux-update>");
        assertTrue(reasons.isEmpty());
    }
}
