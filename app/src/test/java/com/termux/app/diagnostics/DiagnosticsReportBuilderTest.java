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

    private static final DiagnosticsMemoryUsage NO_MEMORY_USAGE = new DiagnosticsMemoryUsage(0, 0, 0, 0);
    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);
    private static final DiagnosticsMainThreadStalls NO_MAIN_THREAD_STALLS =
        new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", java.util.Collections.emptyList());

    private DiagnosticsReport reportWith(List<DiagnosticsSessionLine> sessionLines,
                                         int countedTowardCap, int displayedCount, int maxCap,
                                         int openTabCount, int tabHistoryEntryCount,
                                         boolean wakeLockHeld, boolean foreground,
                                         List<DiagnosticEvent> events) {
        return reportWith(sessionLines, countedTowardCap, displayedCount, maxCap,
            openTabCount, tabHistoryEntryCount, wakeLockHeld, foreground, events,
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST, NO_MAIN_THREAD_STALLS, 0L);
    }

    private DiagnosticsReport reportWith(List<DiagnosticsSessionLine> sessionLines,
                                         int countedTowardCap, int displayedCount, int maxCap,
                                         int openTabCount, int tabHistoryEntryCount,
                                         boolean wakeLockHeld, boolean foreground,
                                         List<DiagnosticEvent> events,
                                         DiagnosticsMemoryUsage memoryUsage,
                                         DiagnosticsWorkCostLine backgroundOutputScanCost,
                                         DiagnosticsWorkCostLine bufferReflowCost,
                                         DiagnosticsMainThreadStalls mainThreadStalls,
                                         long processUptimeMillis) {
        return new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            countedTowardCap, displayedCount, maxCap, sessionLines,
            openTabCount, tabHistoryEntryCount, wakeLockHeld, foreground, events,
            memoryUsage, backgroundOutputScanCost, NO_WORK_COST, bufferReflowCost, mainThreadStalls,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            processUptimeMillis);
    }

    private DiagnosticsReport reportWithMainLooperQueue(DiagnosticsMainLooperQueue mainLooperQueue) {
        return new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST, NO_WORK_COST, NO_MAIN_THREAD_STALLS,
            mainLooperQueue, 0L);
    }

    private DiagnosticsReport reportWithForegroundOpenTagScanCost(DiagnosticsWorkCostLine cost) {
        return new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, cost, NO_WORK_COST, NO_MAIN_THREAD_STALLS,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), 0L);
    }

    @Test
    public void openTagScanOnTheViewedSessionIsReportedWithItsOwnCountTotalAndMaximum() {
        String text = new DiagnosticsReportBuilder().build(
            reportWithForegroundOpenTagScanCost(new DiagnosticsWorkCostLine(4200, 9100, 37, 1800)));

        int sectionIndex = text.indexOf("Open-tag scan on the viewed session");
        Assert.assertTrue("The viewed session's own scan must be reported, otherwise its cost is invisible"
            + " and the reported main-thread total understates the work: " + text, sectionIndex >= 0);
        String section = text.substring(sectionIndex);

        Assert.assertTrue("How many times the scan ran must be stated: " + section,
            section.contains("Count: 4200"));
        Assert.assertTrue("The accumulated cost must be stated: " + section,
            section.contains("Total: 9100 ms"));
        Assert.assertTrue("The worst single scan must be stated: " + section,
            section.contains("Max: 37 ms"));
        Assert.assertTrue("The transcript size at the worst scan must be stated so the growth with the"
                + " transcript is visible: " + section,
            section.contains("Transcript rows at max: 1800"));
    }

    @Test
    public void openTagScanOnTheViewedSessionIsReportedSeparatelyFromTheBackgroundScan() {
        String text = new DiagnosticsReportBuilder().build(
            reportWithForegroundOpenTagScanCost(new DiagnosticsWorkCostLine(1, 2, 3, 4)));

        Assert.assertTrue("The two scans run on different code paths and must not be merged into one"
                + " number: " + text,
            text.indexOf("Background output tag scan") < text.indexOf("Open-tag scan on the viewed session"));
    }

    @Test
    public void mainLooperQueueSectionStatesHowManyMessagesAreWaitingAndWhoTheyBelongTo() {
        DiagnosticsMainLooperQueue mainLooperQueue = DiagnosticsMainLooperQueue.parse(Arrays.asList(
            "Looper (main, tid 2) {b1a2c3}",
            "  Message 0: { when=+1ms callback=com.termux.app.SessionSweep target=android.os.Handler }",
            "  Message 1: { when=+2ms callback=com.termux.app.SessionSweep target=android.os.Handler }",
            "  Message 2: { when=+3ms what=0 target=android.view.Choreographer$FrameHandler }"));

        String text = new DiagnosticsReportBuilder().build(reportWithMainLooperQueue(mainLooperQueue));

        Assert.assertTrue("The pending count must be stated so a saturated queue is visible: " + text,
            text.contains("Pending messages: 3"));
        Assert.assertTrue("The busiest target must be named so the code filling the queue is identified: " + text,
            text.contains("2 x android.os.Handler com.termux.app.SessionSweep"));
        Assert.assertTrue("Every reported target must be listed with its own count: " + text,
            text.contains("1 x android.view.Choreographer$FrameHandler"));
    }

    @Test
    public void mainLooperQueueSectionStatesAnEmptyQueueExplicitly() {
        String text = new DiagnosticsReportBuilder().build(
            reportWithMainLooperQueue(DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList())));

        Assert.assertTrue("An empty queue must still be stated rather than omitted: " + text,
            text.contains("Pending messages: 0"));
        Assert.assertTrue("An empty queue must say so instead of leaving the reader guessing: " + text,
            text.contains("Busiest targets: none"));
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
        lines.add(new DiagnosticsSessionLine("host-a", true, 12, true, 0, 80));
        lines.add(new DiagnosticsSessionLine("host-b", false, 0, false, 0, 80));
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

    @Test
    public void memorySectionShowsJavaAndNativeHeapInWholeMegabytes() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            new DiagnosticsMemoryUsage(187, 224, 512, 96), NO_WORK_COST, NO_WORK_COST,
            NO_MAIN_THREAD_STALLS, 0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("Java heap used must be reported so a reader can see whether the live heap has grown"
                + " large enough to make every garbage collection long: " + text,
            text.contains("Java heap used: 187 MB"));
        Assert.assertTrue("Java heap total must be reported so used can be read against the currently committed heap: "
                + text,
            text.contains("Java heap total: 224 MB"));
        Assert.assertTrue("Java heap max must be reported so a reader can see how close the process is to the heap"
                + " ceiling, which is when collections turn pathological: " + text,
            text.contains("Java heap max: 512 MB"));
        Assert.assertTrue("Native heap allocated must be reported because it is not covered by the Java heap figures"
                + " and still counts against the process memory budget: " + text,
            text.contains("Native heap allocated: 96 MB"));
    }

    @Test
    public void sessionLineShowsTranscriptRowsAndColumns() {
        List<DiagnosticsSessionLine> lines = new ArrayList<>();
        lines.add(new DiagnosticsSessionLine("host-a", true, 12, true, 4213, 92));
        DiagnosticsReport report = reportWith(lines, 1, 1, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("Per-session transcript rows and columns must be reported because the cost of the main-thread"
                + " transcript scan is proportional to them, so they are what makes a slow scan attributable: " + text,
            text.contains("- host-a | alive | last activity: 12s ago | transcript rows: 4213 | columns: 92"));
    }

    @Test
    public void sessionsSectionShowsTotalTranscriptRowsAcrossAllSessions() {
        List<DiagnosticsSessionLine> lines = new ArrayList<>();
        lines.add(new DiagnosticsSessionLine("host-a", true, 1, true, 4213, 92));
        lines.add(new DiagnosticsSessionLine("host-b", true, 2, true, 1787, 92));
        DiagnosticsReport report = reportWith(lines, 2, 2, 32,
            0, 0, false, true, Collections.emptyList());

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("The total across sessions must be reported because the heap held by scrollback is a"
                + " whole-process quantity, and a reader should not have to add the per-session rows by hand: " + text,
            text.contains("Total transcript rows: 6000"));
    }

    @Test
    public void mainThreadCostSectionShowsScanAndReflowCounters() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE,
            new DiagnosticsWorkCostLine(1204, 8600, 41, 4213),
            new DiagnosticsWorkCostLine(9, 730, 213, 3980),
            NO_MAIN_THREAD_STALLS,
            0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("The scan counter must be labelled as the background output tag scan so the reader can tell"
                + " which of the two candidate mechanisms the numbers below belong to: " + text,
            text.contains("Background output tag scan"));
        Assert.assertTrue("The scan count must be reported because a per-scan cost is only meaningful against how"
                + " often the scan ran: " + text,
            text.contains("Count: 1204"));
        Assert.assertTrue("The accumulated scan time must be reported because it is the share of main-thread time the"
                + " scan has taken away from drawing frames: " + text,
            text.contains("Total: 8600 ms"));
        Assert.assertTrue("The worst single scan must be reported because a single long main-thread block is what the"
                + " user perceives as a stutter, which an average would hide: " + text,
            text.contains("Max: 41 ms"));
        Assert.assertTrue("The transcript row count at the worst scan must be reported because it is the evidence that"
                + " links the cost to accumulated scrollback rather than to something else: " + text,
            text.contains("Transcript rows at max: 4213"));

        Assert.assertTrue("The reflow counter must be labelled as the column-changing resize reflow so it is not"
                + " confused with the scan counter that precedes it: " + text,
            text.contains("Buffer reflow on column-changing resize"));
        Assert.assertTrue("The reflow count must be reported because a font size change is rare, and a large cost"
                + " spread over few reflows means something different from the same cost spread over many: " + text,
            text.contains("Count: 9"));
        Assert.assertTrue("The accumulated reflow time must be reported so it can be compared with the scan total to"
                + " decide which mechanism dominates: " + text,
            text.contains("Total: 730 ms"));
        Assert.assertTrue("The worst single reflow must be reported because a font size change that blocks the main"
                + " thread for hundreds of milliseconds is exactly the reported symptom: " + text,
            text.contains("Max: 213 ms"));
        Assert.assertTrue("The transcript row count at the worst reflow must be reported because the reflow walks"
                + " every transcript row, so the row count is what explains the duration: " + text,
            text.contains("Transcript rows at max: 3980"));
    }

    @Test
    public void workCostWithNoSamplesReportsMaxAsNotApplicable() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST, NO_MAIN_THREAD_STALLS, 0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("A counter that never ran must show its zero count rather than be omitted, because knowing"
                + " a mechanism never fired is itself evidence that rules it out: " + text,
            text.contains("Count: 0"));
        Assert.assertTrue("With no samples the maximum must read n/a rather than 0 ms, because 0 ms would be read as a"
                + " measured result showing the work is free: " + text,
            text.contains("Max: n/a"));
        Assert.assertFalse("The transcript row count at the maximum must be omitted when no maximum exists, because"
                + " printing 0 rows would look like a real observation: " + text,
            text.contains("Transcript rows at max"));
    }

    @Test
    public void headerShowsProcessUptime() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST, NO_MAIN_THREAD_STALLS, 45296000L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("Process uptime must be reported because every counter in this report accumulates over the"
                + " process lifetime, so the totals cannot be interpreted without knowing how long that has been: "
                + text,
            text.contains("Process uptime: 12h 34m 56s"));
    }

    @Test
    public void mainThreadStallSectionRanksTheCodePathsThatBlockedTheMainThreadTheLongest() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsMainThreadStalls(80L, 31L, 900L,
                "com.termux.app.Rare.once(Rare.java:1)",
                java.util.Arrays.asList(
                    new MainThreadStallHotPath(
                        "com.termux.app.Frequent.everyFrame(Frequent.java:7)", 30L, 3000L, 140L),
                    new MainThreadStallHotPath(
                        "com.termux.app.Rare.once(Rare.java:1)", 1L, 900L, 900L))),
            0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("The heading must announce the ranking so the reader knows the order is by blocked time: "
                + text,
            text.contains("Blocking the main thread the longest"));
        Assert.assertTrue("The path that consumed the most total main thread time must be reported with its total,"
                + " its occurrence count and its worst single occurrence: " + text,
            text.contains("3000 ms total over 30 stalls, longest 140 ms"));
        Assert.assertTrue("A path is useless without the code it names: " + text,
            text.contains("com.termux.app.Frequent.everyFrame"));
        Assert.assertTrue("The rarer path must still be reported so a one-off freeze is not hidden: " + text,
            text.contains("900 ms total over 1 stalls, longest 900 ms"));
        Assert.assertTrue("The frequent path must be printed before the rare one: " + text,
            text.indexOf("3000 ms total over 30 stalls") < text.indexOf("900 ms total over 1 stalls"));
    }

    @Test
    public void mainThreadStallSectionNamesTheCodeTheLongestStallWasRunning() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsMainThreadStalls(250L, 12L, 4300L,
                "com.termux.app.SlowThing.doWork(SlowThing.java:42)\nandroid.os.Looper.loop(Looper.java:223)",
                Collections.emptyList()),
            0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("The stall threshold must be stated so the reader knows what duration was counted: " + text,
            text.contains("Stalls over 250 ms"));
        Assert.assertFalse("With no aggregated path there is nothing to rank, so the heading must be absent: " + text,
            text.contains("Blocking the main thread the longest"));
        Assert.assertTrue("The stall count must be reported because a high count is what distinguishes a blocked main"
                + " thread from an idle one: " + text,
            text.contains("Count: 12"));
        Assert.assertTrue("The longest stall duration must be reported because it bounds how long a single frame was"
                + " blocked: " + text,
            text.contains("Longest: 4300 ms"));
        Assert.assertTrue("The captured stack must name the blocking code, which is the only way this report can"
                + " identify the cause rather than restate that a stall happened: " + text,
            text.contains("com.termux.app.SlowThing.doWork(SlowThing.java:42)"));
        Assert.assertTrue("Every captured frame must be printed so the caller chain is readable: " + text,
            text.contains("android.os.Looper.loop(Looper.java:223)"));
    }

    @Test
    public void mainThreadStallSectionWithNoStallReportsNoLongestStall() {
        DiagnosticsReport report = reportWith(Collections.emptyList(), 0, 0, 32,
            0, 0, false, true, Collections.emptyList(),
            NO_MEMORY_USAGE, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", java.util.Collections.emptyList()),
            0L);

        String text = new DiagnosticsReportBuilder().build(report);

        Assert.assertTrue("A zero stall count must be printed, because knowing the main thread was never blocked"
                + " rules out main thread blocking as the cause: " + text,
            text.contains("Count: 0"));
        Assert.assertTrue("With no stall the longest must read n/a rather than 0 ms, which would look like a measured"
                + " result: " + text,
            text.contains("Longest: n/a"));
    }
}
