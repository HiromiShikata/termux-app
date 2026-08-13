package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ActivityWindowsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Activity window builds since the app started";

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
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aWindowBuiltRepeatedlyIsReportedWithTheNumberOfBuilds() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(7, 3));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the longest main thread stalls in the readings are view inflation and first"
                + " draw, which are the cost of building the activity window, and paying them once at"
                + " start up cannot be told apart from paying them repeatedly without this count."
                + " Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("Actual report:\n" + report, section.contains("Built: 7"));
    }

    @Test
    public void theTeardownCountIsShownBesideTheBuildCount() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(7, 3));

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("a build count on its own cannot distinguish an application that is recreating"
            + " its window from one that was rebuilt after being torn down. Actual report:\n" + report,
            section.contains("Torn down: 3"));
        Assert.assertTrue("a build whose teardown never ran leaves that window's views attached, which is"
            + " a different fact from an activity retained after its teardown ran, and only the first of"
            + " those two is visible here. Actual report:\n" + report,
            section.contains("Teardown not run: 4"));
    }

    @Test
    public void aProcessThatBuiltItsWindowOnceSaysSoRatherThanBeingLeftBlank() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(1, 0));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("one build and no teardown is the healthy state, and a reader must be able to"
                + " confirm it rather than infer it from an absent section. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("Actual report:\n" + report,
            report.substring(sectionIndex).contains("Built: 1"));
    }

    @Test
    public void aReadingTakenBeforeTheWindowWasBuiltIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsActivityWindows.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is exactly the state"
                + " a reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no build having happened is itself a finding and has to be stated. Actual"
                + " report:\n" + report,
            report.substring(sectionIndex).contains("None: the activity window has not been built yet"));
    }

    @Test
    public void theBuildCountSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(7, 3));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the count has to be present for its position to matter. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only the"
                + " first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a count that"
                + " falls outside that window cannot be read at all. It currently begins at " + sectionIndex
                + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
