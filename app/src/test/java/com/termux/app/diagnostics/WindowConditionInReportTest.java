package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class WindowConditionInReportTest {

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
            DiagnosticsTouchEvents.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aReadingStatesHowLongAgoTheTerminalDrewAndWhatConditionTheWindowWasIn() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(2, 1)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.drawnMillisAgo(4_120L),
                DiagnosticsDrawTime.drawnMillisAgo(1_843L), "VISIBLE", true, true));

        String report = renderedReport(activityWindows);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("a reading taken while the interface is frozen has to say whether frames were"
            + " still being produced, and there is nothing in the reading that says so today. Actual"
            + " report:\n" + report, sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the age of the last frame is what separates a stopped frame pipeline from a"
                + " pipeline that is running and simply has nothing to show. Actual report:\n" + report,
            section.contains("Terminal last drew: 1843 ms ago"));
        Assert.assertTrue("a window the platform has stopped cannot be told from a live one without"
                + " this. Actual report:\n" + report, section.contains("Window visibility: VISIBLE"));
        Assert.assertTrue("a decor view detached from its window receives no traversal at all. Actual"
            + " report:\n" + report, section.contains("Attached to a window: yes"));
        Assert.assertTrue("a window without focus is a window something else is in front of, which the"
                + " owner is not looking at. Actual report:\n" + report,
            section.contains("Has window focus: yes"));
    }

    @Test
    public void aWindowThatIsNotVisibleAndNotFocusedIsStatedAsSuchRatherThanOmitted() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(2, 1)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.drawnMillisAgo(94_002L),
                DiagnosticsDrawTime.drawnMillisAgo(94_002L), "GONE", true, false));

        String report = renderedReport(activityWindows);

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("a frame pipeline that last ran a minute and a half ago is the whole finding,"
                + " and rounding or omitting it loses it. Actual report:\n" + report,
            section.contains("Terminal last drew: 94002 ms ago"));
        Assert.assertTrue("Actual report:\n" + report, section.contains("Window visibility: GONE"));
        Assert.assertTrue("Actual report:\n" + report, section.contains("Has window focus: no"));
    }

    @Test
    public void aTerminalThatHasNotDrawnYetSaysSoRatherThanReportingNoDelay() {
        DiagnosticsActivityWindows activityWindows = new DiagnosticsActivityWindows(1, 0)
            .withCondition(DiagnosticsWindowCondition.measured(
                DiagnosticsDrawTime.NEVER_DRAWN, DiagnosticsDrawTime.NEVER_DRAWN, "VISIBLE", true,
                false));

        String report = renderedReport(activityWindows);

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("zero milliseconds since a draw that never happened reads as a frame produced"
                + " this instant. Actual report:\n" + report,
            section.contains("Terminal last drew: not since the process started"));
    }

    @Test
    public void aReadingTakenWithNoWindowToLookAtSaysItWasNotMeasured() {
        String report = renderedReport(new DiagnosticsActivityWindows(2, 1));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an omitted section is indistinguishable from a build that never measured the"
            + " window. Actual report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("a window that could not be read would otherwise report as not visible, which"
                + " is a measurement nobody took. Actual report:\n" + report,
            report.substring(sectionIndex).contains("Not measured"));
    }

    @Test
    public void theWindowConditionSurvivesInsideThePasteWindow() {
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
            window.contains("Terminal last drew: 1843 ms ago"));
    }
}
