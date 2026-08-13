package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ActivityWindowsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Activity windows since the app started";

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
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, activityWindows);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aWindowTheProcessNeverDestroyedIsNamedAsStillHeld() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(7, 3));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("every window in the process posts its scrollbar fade callbacks to the same main"
                + " looper, so a reading showing far more of those callbacks than one window can hold cannot"
                + " be told apart from a wrong reading without this. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the number still held is the finding a reader acts on. Actual report:\n" + report,
            section.contains("Still held: 4"));
    }

    @Test
    public void theCreatedAndDestroyedCountsAreBothShownSoTheDifferenceCanBeChecked() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(7, 3));

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("a difference with no terms behind it cannot be checked against the rest of the"
            + " reading. Actual report:\n" + report, section.contains("Created: 7"));
        Assert.assertTrue("without the destroyed count a process that recreates its window often looks the"
            + " same as one that leaks them. Actual report:\n" + report, section.contains("Destroyed: 3"));
    }

    @Test
    public void aProcessHoldingExactlyOneWindowSaysSoRatherThanBeingLeftBlank() {
        String report = renderedReportOf(new DiagnosticsActivityWindows(1, 0));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("one held window is the healthy state, and a reader must be able to confirm it"
                + " rather than infer it from an absent section. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("Actual report:\n" + report,
            report.substring(sectionIndex).contains("Still held: 1"));
    }

    @Test
    public void aReadingTakenBeforeAnyWindowWasCreatedIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsActivityWindows.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is exactly the state"
                + " a reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no window having been created is itself a finding and has to be stated. Actual"
                + " report:\n" + report,
            report.substring(sectionIndex).contains("None: no activity window has been created yet"));
    }

    @Test
    public void theActivityWindowCountSitsInsideTheWindowTheReportSurvives() {
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
