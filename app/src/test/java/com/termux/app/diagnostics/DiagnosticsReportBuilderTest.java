package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiagnosticsReportBuilderTest {

    private static final long REPORT_MILLIS = 1783216800000L;
    private static final long EVENT_MILLIS = 1783216770000L;

    private DiagnosticsReport reportWith(List<DiagnosticsSessionLine> sessionLines,
                                         int countedTowardCap, int displayedCount, int maxCap,
                                         int openTabCount, int tabHistoryEntryCount,
                                         boolean wakeLockHeld, boolean foreground,
                                         List<DiagnosticEvent> events) {
        return new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            countedTowardCap, displayedCount, maxCap, sessionLines,
            openTabCount, tabHistoryEntryCount, wakeLockHeld, foreground, events);
    }

    @Test
    public void headerContainsVersionAndTimestamp() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("App version: 0.119.0 (119)"));
        Assert.assertTrue(text.contains("Generated: 2026-07-05T02:00:00Z"));
    }

    @Test
    public void sessionsSectionShowsCountsAndCap() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 5, 5, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("Counted toward cap: 5"));
        Assert.assertTrue(text.contains("Displayed in list: 5"));
        Assert.assertTrue(text.contains("Max sessions cap: 32"));
    }

    @Test
    public void orphanedLineShownOnlyWhenCountsDiffer() {
        DiagnosticsReport equalCounts = reportWith(Collections.emptyList(), 23, 23, 32,
            0, 0, false, true, Collections.emptyList());
        Assert.assertFalse(new DiagnosticsReportBuilder().build(equalCounts).contains("Orphaned"));

        DiagnosticsReport differingCounts = reportWith(Collections.emptyList(), 32, 23, 32,
            0, 0, false, true, Collections.emptyList());
        String text = new DiagnosticsReportBuilder().build(differingCounts);
        Assert.assertTrue(text.contains("Orphaned (counted but not displayed): 9"));
    }

    @Test
    public void sessionLineShowsNameAliveStateAndSecondsSinceActivity() {
        List<DiagnosticsSessionLine> lines = new ArrayList<>();
        lines.add(new DiagnosticsSessionLine("host-a", true, 12, true));
        lines.add(new DiagnosticsSessionLine("host-b", false, 0, false));
        DiagnosticsReport report = reportWith(lines, 2, 2, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("- host-a | alive | last activity: 12s ago"));
        Assert.assertTrue(text.contains("- host-b | dead | last activity: n/a"));
    }

    @Test
    public void noSessionsRendersPlaceholder() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("(no sessions)"));
    }

    @Test
    public void browserSectionShowsTabAndHistoryCounts() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            4, 137, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("Open tabs: 4"));
        Assert.assertTrue(text.contains("Tab-history entries: 137"));
    }

    @Test
    public void wakeLockSectionShowsHeldAndForegroundState() {
        DiagnosticsReport heldForeground = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, true, true, Collections.emptyList());
        String heldText = new DiagnosticsReportBuilder().build(heldForeground);
        Assert.assertTrue(heldText.contains("Held: yes"));
        Assert.assertTrue(heldText.contains("App state: foreground"));

        DiagnosticsReport releasedBackground = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, false, Collections.emptyList());
        String releasedText = new DiagnosticsReportBuilder().build(releasedBackground);
        Assert.assertTrue(releasedText.contains("Held: no"));
        Assert.assertTrue(releasedText.contains("App state: background"));
    }

    @Test
    public void eventsSectionShowsTailWithTimestampTypeAndDetail() {
        List<DiagnosticEvent> events = Arrays.asList(
            new DiagnosticEvent(EVENT_MILLIS, DiagnosticEventType.SESSION_CREATED, "host-a"),
            new DiagnosticEvent(EVENT_MILLIS, DiagnosticEventType.MAX_SESSIONS_REACHED, "cap=32"));
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, events);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("2026-07-05T01:59:30Z SESSION_CREATED host-a"));
        Assert.assertTrue(text.contains("2026-07-05T01:59:30Z MAX_SESSIONS_REACHED cap=32"));
    }

    @Test
    public void noEventsRendersPlaceholder() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("(no recent events)"));
    }

    @Test
    public void eventWithEmptyDetailOmitsTrailingSpace() {
        List<DiagnosticEvent> events = Collections.singletonList(
            new DiagnosticEvent(EVENT_MILLIS, DiagnosticEventType.WAKE_LOCK_RELEASED, ""));
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, false, events);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue(text.contains("2026-07-05T01:59:30Z WAKE_LOCK_RELEASED\n"));
    }
}
