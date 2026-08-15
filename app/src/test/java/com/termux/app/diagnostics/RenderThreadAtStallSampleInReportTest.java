package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class RenderThreadAtStallSampleInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String STALL_SECTION_HEADING = "Stalls over ";

    private static String renderedReportOf(DiagnosticsMainThreadStalls mainThreadStalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            mainThreadStalls,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE,
            DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsMainThreadStalls stallsWithOneStall() {
        return new DiagnosticsMainThreadStalls(250L, 1L, 500L,
            "com.termux.app.Blocking.reflowBuffer(Blocking.java:42)",
            Collections.<MainThreadStallHotPath>emptyList(), 1L, 0L);
    }

    @Test
    public void aSampledStallCarriesTheRenderThreadProcessorTimeAndStateFromTheSample() {
        String report = renderedReportOf(stallsWithOneStall()
            .withMaxStallRenderThread(DiagnosticsRenderThread.measured("R", 190120L, 4210L)));

        int stallSectionIndex = report.indexOf(STALL_SECTION_HEADING);
        Assert.assertTrue("the stall section must be present for its content to be judged. Actual report:\n"
            + report, stallSectionIndex >= 0);
        String stallSection = report.substring(stallSectionIndex);

        Assert.assertTrue("the processor time the render thread accumulated by sample time separates a"
                + " thread that was saturated from one that was idle, which is the evidence needed to"
                + " attribute the stall to the render thread rather than to the main thread alone."
                + " Actual report:\n" + report,
            stallSection.contains("190120 ms user, 4210 ms system"));
        Assert.assertTrue("the scheduler state at sample time separates a render thread that was running"
                + " from one that was itself blocked or parked, which names a different culprit."
                + " Actual report:\n" + report,
            stallSection.contains("state R"));
    }

    @Test
    public void aStallSampledWhileTheRenderThreadCouldNotBeReadSaysWhyRatherThanReadingAsNoProcessorTime() {
        String report = renderedReportOf(stallsWithOneStall()
            .withMaxStallRenderThread(
                DiagnosticsRenderThread.readFailed("the thread table could not be read during the stall")));

        int stallSectionIndex = report.indexOf(STALL_SECTION_HEADING);
        Assert.assertTrue("the stall section must be present for its content to be judged. Actual report:\n"
            + report, stallSectionIndex >= 0);
        String stallSection = report.substring(stallSectionIndex);

        Assert.assertTrue("a failure printed as zero processor time would have the reader rule out a"
                + " saturated render thread on the strength of a reading that never happened."
                + " Actual report:\n" + report,
            stallSection.contains("the thread table could not be read during the stall"));
    }

    @Test
    public void theRenderThreadAtSampleTimeSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(stallsWithOneStall()
            .withMaxStallRenderThread(DiagnosticsRenderThread.measured("R", 190120L, 4210L)));

        int lineIndex = report.indexOf("190120 ms user");
        Assert.assertTrue("the line must be present for its position to be judged. Actual report:\n"
            + report, lineIndex >= 0);
        Assert.assertTrue("the report is pasted into a channel that keeps only the first "
                + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a line past that"
                + " point never reaches the reader. It currently begins at character " + lineIndex
                + ". Actual report:\n" + report,
            lineIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
