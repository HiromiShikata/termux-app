package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class RenderThreadInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Render thread";

    private static final String BACKGROUND_CYCLE_SECTION_HEADING = "Background cycle";

    private static String renderedReportOf(DiagnosticsMainThreadStalls mainThreadStalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            mainThreadStalls,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, new DiagnosticsActivityWindows(1, 0),
            DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsMainThreadStalls stallsWithoutARenderThreadReading() {
        return new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L);
    }

    @Test
    public void theReportNamesTheRenderThreadSoAStallSpentWaitingOnItIsNotLeftUnattributable() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading());

        Assert.assertTrue("the calls that block the main thread inside a traversal hand their work to the"
                + " render thread and wait for it, so a reading that never names that thread records the"
                + " waiter and can never show what was holding it. Actual report:\n" + report,
            report.indexOf(SECTION_HEADING) >= 0);
    }

    @Test
    public void aRenderThreadThatCouldNotBeReadSaysSoRatherThanBeingLeftBlank() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present before what it says about an unread thread can"
            + " mean anything. Actual report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("a silently absent measurement reads as a render thread that used no processor"
                + " time, which is the opposite of the conclusion the reader would draw from a saturated"
                + " one. Actual report:\n" + report,
            report.substring(sectionIndex).contains("Not measured"));
    }

    @Test
    public void theRenderThreadIsPrintedAheadOfTheBackgroundCycleSectionSoTheCeilingDoesNotDropIt() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        int backgroundCycleIndex = report.indexOf(BACKGROUND_CYCLE_SECTION_HEADING);
        Assert.assertTrue("both sections have to be present for their order to mean anything. Actual"
            + " report:\n" + report, sectionIndex >= 0 && backgroundCycleIndex >= 0);
        Assert.assertTrue("the reading that carried this evidence was cut off at the paste ceiling part"
                + " way through the background cycle section, so a render thread printed after it would"
                + " have been lost in exactly the capture it was added for. Actual report:\n" + report,
            sectionIndex < backgroundCycleIndex);
    }

    @Test
    public void aMeasuredRenderThreadIsPrintedWithItsProcessorTimeAndItsState() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading()
            .withRenderThread(DiagnosticsRenderThread.measured("R", 190120L, 4210L)));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present before its content can be judged. Actual"
            + " report:\n" + report, sectionIndex >= 0);
        String section = report.substring(sectionIndex);
        Assert.assertTrue("the processor time against the process uptime is what says whether the"
                + " thread the main thread waited on was saturated or was itself blocked. Actual"
                + " report:\n" + report,
            section.contains("190120 ms user, 4210 ms system"));
        Assert.assertTrue("the state at the moment of the reading separates a thread that is running"
            + " from one that is parked. Actual report:\n" + report, section.contains("State: R"));
    }

    @Test
    public void aRenderThreadReadingThatFailedPrintsWhyRatherThanReadingAsNoProcessorTime() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading()
            .withRenderThread(DiagnosticsRenderThread.readFailed("the thread table could not be listed")));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present before its content can be judged. Actual"
            + " report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("a failure printed as a blank or a zero would have the reader rule out a"
                + " saturated render thread on a reading that never happened. Actual report:\n" + report,
            report.substring(sectionIndex).contains("the thread table could not be listed"));
    }

    @Test
    public void theRenderThreadSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(stallsWithoutARenderThreadReading());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present before its position can be judged. Actual"
            + " report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("the reading is pasted into a session and truncated at "
                + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a section printed"
                + " past that point never reaches the reader. Actual index: " + sectionIndex,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
