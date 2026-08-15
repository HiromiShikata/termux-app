package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class PreviousProcessConditionInReportTest {

    private static final long REPORT_MILLIS = 1786968550000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Condition of the previous process before it ended";

    private static String renderedReportOf(ProcessConditionSnapshot previousProcessCondition) {
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
            DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN, previousProcessCondition);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportStatesWhatTheProcessThatEndedHadMeasuredAboutItself() {
        String report = renderedReportOf(ProcessConditionSnapshot.recorded(1786968180000L, 342000L,
            12, 1, 82, 1786968102000L, 71, 3));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the condition being investigated is one in which the application stops"
                + " responding, and a reading cannot be taken then because taking one means operating"
                + " the application, so the process is force-stopped and the numbers it held die with"
                + " it. Without this section every report describes a process that started healthy"
                + " after the event. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("without the moment the record was written, the numbers cannot be placed"
                + " against the moment the process ended. Actual report:\n" + report,
            section.contains("Last recorded: 2026-08-17T12:03:00Z"));
        Assert.assertTrue("how long that process had been alive separates a process that degraded"
                + " over hours from one that failed at startup. Actual report:\n" + report,
            section.contains("Process uptime then: 0h 5m 42s"));
        Assert.assertTrue("the queue depth at the last recorded moment is what a held main looper"
                + " shows. Actual report:\n" + report,
            section.contains("Main looper pending messages then: 12"));
        Assert.assertTrue("a synchronization barrier holds every ordinary message behind it, so its"
                + " presence separates a blocked looper from a busy one. Actual report:\n" + report,
            section.contains("Synchronization barriers in the queue then: 1"));
        Assert.assertTrue("the deepest queue that process ever saw is the reading this whole"
                + " investigation turns on, and it is lost with the process. Actual report:\n" + report,
            section.contains("Deepest main looper queue it saw: 82 at 2026-08-17T12:01:42Z"));
        Assert.assertTrue("the count of views that could hold a scrollbar fade callback at that"
                + " depth is what separates leaked fade callbacks from an ordinary queue."
                + " Actual report:\n" + report,
            section.contains("Views that could hold a scrollbar fade callback then: 71"));
        Assert.assertTrue("a scroll gesture the terminal never drew for is the symptom itself, so"
                + " its count in the process that ended is the evidence that an episode happened."
                + " Actual report:\n" + report,
            section.contains("Scroll gestures the terminal never drew for (up to 3 kept): 3"));
    }

    @Test
    public void aRunWithNoEarlierRecordSaysSoRatherThanPrintingZeroes() {
        String report = renderedReportOf(ProcessConditionSnapshot.NOT_RECORDED);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as a report that never carried this evidence,"
                + " which is indistinguishable from a build without the record. Actual report:\n"
                + report,
            sectionIndex >= 0);
        Assert.assertTrue("zeroes for a process that never wrote a record would be read as a"
                + " measured quiet process. Actual report:\n" + report,
            report.substring(sectionIndex).contains("None: no earlier process recorded its condition"));
    }

    @Test
    public void aRecordThatCouldNotBeReadSaysWhyRatherThanLookingAbsent() {
        String report = renderedReportOf(
            ProcessConditionSnapshot.unreadable("the record holds a token without a name and a value"));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("a failure to read the record must not be silent, or the evidence looks"
                + " never to have been written. Actual report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("the reason the record could not be read is what tells a reader whether"
                + " the writing side or the reading side is at fault. Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("Unreadable: the record holds a token without a name and a value"));
    }

    @Test
    public void theConditionOfTheProcessThatEndedSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(ProcessConditionSnapshot.recorded(1786968180000L, 342000L,
            12, 1, 82, 1786968102000L, 71, 3));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps"
                + " only the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters,"
                + " so a section outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
