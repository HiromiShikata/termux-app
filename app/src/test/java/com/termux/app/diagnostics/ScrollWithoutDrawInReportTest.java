package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ScrollWithoutDrawInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Scrolled without the terminal drawing";

    private static final String EVENTS_SECTION_HEADING = "Recent events";

    private static String renderedReportOf(DiagnosticsActivityWindows activityWindows) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, activityWindows, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsActivityWindows windowsWithEpisode() {
        return new DiagnosticsActivityWindows(1, 0).withScrollWithoutDrawEpisodes(
            Collections.singletonList(new ScrollWithoutDrawEpisode(REPORT_MILLIS - 5_000L, 31_000L)));
    }

    @Test
    public void aRecordedEpisodeIsPrintedWithHowLongTheTerminalHadNotDrawn() {
        String report = renderedReportOf(windowsWithEpisode());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the reading is produced through the interface that stops responding, so unless"
                + " the episode is printed from the record the report can only ever be taken once the"
                + " terminal is drawing again and shows nothing. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("the gap is what separates a stopped frame pipeline from a slow one, so the"
                + " section naming the episode without it leaves the reader no more informed. Actual"
                + " report:\n" + report,
            report.substring(sectionIndex).contains("31000 ms after the terminal last drew"));
    }

    @Test
    public void aTerminalThatHasDrawnSinceItWasScrolledSaysSoRatherThanBeingLeftBlank() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(1, 0));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as a build that cannot record episodes at all, which is"
                + " exactly the state a reader must be able to rule out before concluding the terminal was"
                + " drawing throughout. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no episode having happened is itself a finding and has to be stated. Actual"
                + " report:\n" + report,
            report.substring(sectionIndex).contains("None: the terminal has drawn since it was last scrolled"));
    }

    @Test
    public void theEpisodeIsPrintedAheadOfTheRecentEventsSection() {
        String report = renderedReportOf(windowsWithEpisode());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        int eventsSectionIndex = report.indexOf(EVENTS_SECTION_HEADING);
        Assert.assertTrue("both sections have to be present for their order to mean anything. Actual"
                + " report:\n" + report,
            sectionIndex >= 0 && eventsSectionIndex >= 0);
        Assert.assertTrue("the report is read by being pasted into a channel that keeps only the first "
                + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, and a capture taken"
                + " during the reported defect was cut mid-word at that limit, so an episode printed in"
                + " the last section is the first evidence lost from exactly the report that carries it."
                + " The episode section begins at " + sectionIndex + " and recent events at "
                + eventsSectionIndex + ". Actual report:\n" + report,
            sectionIndex < eventsSectionIndex);
    }

    @Test
    public void theEpisodeSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(windowsWithEpisode());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the episode has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("a section that falls outside the pasted window cannot be read at all. It"
                + " currently begins at " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
