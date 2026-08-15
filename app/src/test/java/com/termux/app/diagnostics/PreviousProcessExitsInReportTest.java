package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PreviousProcessExitsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Why the recent processes of this app ended";

    private static String renderedReportOf(DiagnosticsPreviousProcessExits previousProcessExits) {
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
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE,
            DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE, previousProcessExits,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aRecordedEndingNamesItsReasonItsTimeAndWhatTheSystemSaidAboutIt() {
        DiagnosticsPreviousProcessExits exits = DiagnosticsPreviousProcessExits.recorded(Arrays.asList(
            new DiagnosticsPreviousProcessExit(1783216740000L, 3, 400, "isolated not needed"),
            new DiagnosticsPreviousProcessExit(1783216680000L, 6, 100, "Input dispatching timed out")));

        String report = renderedReportOf(exits);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the whole session set is being torn down and rebuilt, and nothing the app"
                + " records says whether the process behind it ended or kept running, so the section has"
                + " to exist for the cause to be attributable at all. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("a reason shown as a number leaves the reader to look it up, which is the"
                + " failure this section exists to remove. Actual report:\n" + report,
            section.contains("the system reclaiming memory"));
        Assert.assertTrue("an unresponsive main thread is the symptom under investigation and has to be"
                + " named. Actual report:\n" + report,
            section.contains("an unresponsive main thread"));
        Assert.assertTrue("without the time of the ending it cannot be lined up against the moment the"
                + " sessions were rebuilt. Actual report:\n" + report,
            section.contains("2026-07-05T01:59:00Z"));
        Assert.assertTrue("the system's own words about the ending carry detail no reason code does."
                + " Actual report:\n" + report,
            section.contains("Input dispatching timed out"));
        Assert.assertTrue("whether the process was in front of the user when it ended separates a"
                + " background reclaim from one the user was watching. Actual report:\n" + report,
            section.contains("in the foreground"));
    }

    @Test
    public void anAndroidVersionThatKeepsNoSuchRecordSaysSoRatherThanShowingNothing() {
        String report = renderedReportOf(DiagnosticsPreviousProcessExits.notRecordedByThisAndroid());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an app whose processes never ended, which is the"
                + " one conclusion the reader must not draw by default. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("a version that keeps no record must be stated, so the empty result is not"
                + " read as evidence that nothing ended. Actual report:\n" + report,
            report.substring(sectionIndex).contains("does not keep"));
    }

    @Test
    public void aSystemHoldingNoRecordYetSaysSoRatherThanShowingNothing() {
        String report = renderedReportOf(
            DiagnosticsPreviousProcessExits.recorded(Collections.<DiagnosticsPreviousProcessExit>emptyList()));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its content to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("a record that is genuinely empty is itself a finding, because it says no"
                + " process of this app has ended, and it must be distinguishable from an unread one."
                + " Actual report:\n" + report,
            report.substring(sectionIndex).contains("None:"));
    }

    @Test
    public void aReadingThatWasNeverTakenSaysSoRatherThanLookingMeasured() {
        String report = renderedReportOf(DiagnosticsPreviousProcessExits.NOT_TAKEN);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its content to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("an unread state must not be presentable as a measured one. Actual report:\n"
            + report, report.substring(sectionIndex).contains("Not measured"));
    }

    @Test
    public void endingsTheSystemDescribedAtLengthAreCutRatherThanSwallowingTheRestOfTheReport() {
        StringBuilder longDescription = new StringBuilder();
        while (longDescription.length() < 3000) {
            longDescription.append("the system said a great deal about this ending. ");
        }
        List<DiagnosticsPreviousProcessExit> endings = new ArrayList<>();
        for (int endingIndex = 0; endingIndex < 5; endingIndex++) {
            endings.add(new DiagnosticsPreviousProcessExit(1783216740000L - endingIndex * 60000L,
                4, 100, longDescription.toString()));
        }

        String report = renderedReportOf(DiagnosticsPreviousProcessExits.recorded(endings));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its size to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("a section that prints whatever the system happened to write pushes every"
                + " measurement after it out of the window the report survives, so it has to be cut and"
                + " say that it was cut. Actual report:\n" + report,
            report.substring(sectionIndex).contains("further lines left out"));
        int mainThreadCostIndex = report.indexOf("Main-thread cost");
        Assert.assertTrue("the main-thread cost block is the evidence this report exists to carry, and it"
                + " has to stay inside the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS
                + " characters however much the system wrote about an ending. It currently begins at"
                + " character " + mainThreadCostIndex + ". Actual report:\n" + report,
            mainThreadCostIndex >= 0 && mainThreadCostIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }

    @Test
    public void theEndingsSitInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(DiagnosticsPreviousProcessExits.recorded(Collections.singletonList(
            new DiagnosticsPreviousProcessExit(1783216740000L, 4, 100, "crash"))));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " section that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
