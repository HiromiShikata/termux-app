package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class WindowDrawTimeInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Activity window condition when this reading was taken";

    private static String renderedReport(DiagnosticsActivityWindows activityWindows) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.3877", 3877, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(80L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST, NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE,
            activityWindows, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aReadingStatesHowLongAgoTheWindowItselfCompletedADrawPass() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(2, 1)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.drawnMillisAgo(4_120L),
                DiagnosticsDrawTime.drawnMillisAgo(1_843L), "VISIBLE", true, true));

        String report = renderedReport(activityWindows);

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("without the window's own last draw pass, an old terminal draw cannot be told"
                + " apart from a window that kept drawing while the terminal had nothing to redraw."
                + " Actual report:\n" + report,
            section.contains("Window last drew: 4120 ms ago"));
    }

    @Test
    public void theWindowDrawAgeIsStatedBesideTheTerminalDrawAgeSoTheTwoCanBeCompared() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(2, 1)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.drawnMillisAgo(31L),
                DiagnosticsDrawTime.drawnMillisAgo(94_002L), "VISIBLE", true, true));

        String report = renderedReport(activityWindows);

        String section = report.substring(report.indexOf(SECTION_HEADING));
        int windowLineIndex = section.indexOf("Window last drew: 31 ms ago");
        int terminalLineIndex = section.indexOf("Terminal last drew: 94002 ms ago");
        Assert.assertTrue("a window drawing thirty milliseconds ago while the terminal last drew a minute"
                + " and a half ago rules out a stopped frame pipeline, and that comparison is the whole"
                + " point of the line. Actual report:\n" + report, windowLineIndex >= 0);
        Assert.assertTrue("Actual report:\n" + report, terminalLineIndex >= 0);
        Assert.assertTrue("the two ages must be read together, so they belong next to each other."
            + " Actual report:\n" + report, windowLineIndex < terminalLineIndex);
    }

    @Test
    public void aWindowThatHasNotDrawnYetSaysSoRatherThanReportingNoDelay() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(1, 0)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.NEVER_DRAWN, DiagnosticsDrawTime.NEVER_DRAWN, "VISIBLE", true,
                false));

        String report = renderedReport(activityWindows);

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("zero milliseconds since a draw pass that never happened reads as a window that"
                + " has just drawn. Actual report:\n" + report,
            section.contains("Window last drew: not since the process started"));
    }

    @Test
    public void theWindowDrawAgeSurvivesInsideThePasteWindow() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(2, 1)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.drawnMillisAgo(4_120L),
                DiagnosticsDrawTime.drawnMillisAgo(1_843L), "VISIBLE", true, true));

        String report = renderedReport(activityWindows);

        String window = report.substring(0, Math.min(report.length(),
            DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
        Assert.assertTrue("the reading reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters. Actual"
                + " report:\n" + report,
            window.contains("Window last drew: 4120 ms ago"));
    }
}
