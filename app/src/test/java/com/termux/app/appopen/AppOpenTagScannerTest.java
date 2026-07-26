package com.termux.app.appopen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class AppOpenTagScannerTest {

    @Test
    public void extractsPackageIdOfCompleteBlock() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds("before <app-open>com.example.app</app-open> after");
        assertEquals(1, packageIds.size());
        assertEquals("com.example.app", packageIds.get(0));
    }

    @Test
    public void trimsSurroundingWhitespaceAndNewlinesInsideBlock() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds("<app-open>\n  com.example.app  \n</app-open>");
        assertEquals(1, packageIds.size());
        assertEquals("com.example.app", packageIds.get(0));
    }

    @Test
    public void ignoresBlockWithoutClosingTag() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds("<app-open>com.example.app");
        assertTrue(packageIds.isEmpty());
    }

    @Test
    public void extractsMultipleBlocksNonGreedily() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds(
            "<app-open>com.first.app</app-open> mid <app-open>com.second.app</app-open>");
        assertEquals(2, packageIds.size());
        assertEquals("com.first.app", packageIds.get(0));
        assertEquals("com.second.app", packageIds.get(1));
    }

    @Test
    public void ignoresEmptyBlock() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds("<app-open>   </app-open>");
        assertTrue(packageIds.isEmpty());
    }

    @Test
    public void ignoresValueWithoutAPackageIdShape() {
        List<String> packageIds = AppOpenTagScanner.extractPackageIds(
            "<app-open>notapackage</app-open><app-open>https://example.com</app-open><app-open>.leadingdot</app-open>");
        assertTrue(packageIds.isEmpty());
    }

    @Test
    public void normalizePackageIdRejectsSingleSegmentAndAcceptsDottedId() {
        assertEquals("com.example.app", AppOpenTagScanner.normalizePackageId("com.example.app"));
        assertEquals(null, AppOpenTagScanner.normalizePackageId("singleword"));
        assertEquals(null, AppOpenTagScanner.normalizePackageId("   "));
    }

    @Test
    public void packageIdsToLaunchReturnsEachPackageIdInOrderOnFirstScan() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();
        List<String> packageIds = scanner.packageIdsToLaunch(
            "<app-open>com.first.app</app-open><app-open>com.second.app</app-open>");
        assertEquals(2, packageIds.size());
        assertEquals("com.first.app", packageIds.get(0));
        assertEquals("com.second.app", packageIds.get(1));
    }

    @Test
    public void deduplicatesAlreadyLaunchedPackageIdOnRedraw() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();
        String output = "prompt <app-open>com.example.app</app-open> prompt";

        assertEquals(1, scanner.packageIdsToLaunch(output).size());
        assertTrue(scanner.packageIdsToLaunch(output).isEmpty());
    }

    @Test
    public void launchesNextNewPackageIdAfterPreviousLaunched() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();

        assertEquals(List.of("com.first.app"),
            scanner.packageIdsToLaunch("<app-open>com.first.app</app-open>"));
        assertEquals(List.of("com.second.app"),
            scanner.packageIdsToLaunch("<app-open>com.first.app</app-open><app-open>com.second.app</app-open>"));
        assertTrue(scanner.packageIdsToLaunch(
            "<app-open>com.first.app</app-open><app-open>com.second.app</app-open>").isEmpty());
    }

    @Test
    public void returnsEmptyWhenNoBlockPresent() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();
        assertTrue(scanner.packageIdsToLaunch("plain terminal output").isEmpty());
        assertTrue(scanner.packageIdsToLaunch(null).isEmpty());
    }

    @Test
    public void doesNotReLaunchAnAlreadyLaunchedPackageIdThatRemainsInScrollbackWhenMoreOutputArrives() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();

        assertEquals(List.of("com.example.app"),
            scanner.packageIdsToLaunch("<app-open>com.example.app</app-open>"));

        assertTrue(scanner.packageIdsToLaunch(
            "<app-open>com.example.app</app-open>\nmore output line 1").isEmpty());
        assertTrue(scanner.packageIdsToLaunch(
            "<app-open>com.example.app</app-open>\nmore output line 1\nmore output line 2").isEmpty());
    }

    @Test
    public void launchesAGenuinelyNewPackageIdAfterEarlierOnesHaveBeenTrimmedOutOfTheTranscript() {
        AppOpenTagScanner scanner = new AppOpenTagScanner();

        StringBuilder longTranscript = new StringBuilder("<app-open>com.example.a</app-open>\n");
        for (int line = 0; line < 5000; line++) {
            longTranscript.append("output line ").append(line).append('\n');
        }
        assertEquals(List.of("com.example.a"), scanner.packageIdsToLaunch(longTranscript.toString()));

        String trimmedWithNewTag =
            "output line 4998\noutput line 4999\n<app-open>com.example.b</app-open>\n";
        assertEquals(List.of("com.example.b"), scanner.packageIdsToLaunch(trimmedWithNewTag));
    }
}
