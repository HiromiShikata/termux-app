package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ShellExitStatusInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Shell exits since the app started";

    private static String renderedReportOf(DiagnosticsShellExits shellExits) {
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
            shellExits, DiagnosticsPhantomProcessMonitor.UNMEASURED, DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportNamesWhyEachShellExitedRatherThanOnlyThatAReconnectFollowed() {
        String report = renderedReportOf(new DiagnosticsShellExits(Arrays.asList(
            new DiagnosticsShellExitCount(255, 18),
            new DiagnosticsShellExitCount(0, 2))));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("every background reconnect is attributed to the shell process having exited,"
                + " so without the status the shell exited with there is nothing in the report that"
                + " separates a transport that ended from a command that returned or a process that was"
                + " killed, and those have entirely different fixes. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the total is what tells a reader how much of the session churn this accounts"
            + " for. Actual report:\n" + report, section.contains("Total: 20"));
        Assert.assertTrue("the dominant exit status is the evidence that decides which cause to chase."
            + " Actual report:\n" + report, section.contains("Exit status 255: 18"));
        Assert.assertTrue("a status seen less often still separates one cause from another and must not"
            + " be dropped. Actual report:\n" + report, section.contains("Exit status 0: 2"));
    }

    @Test
    public void aRunInWhichNoShellExitedIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsShellExits.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is exactly the"
                + " state a reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("nothing having exited is itself a finding and has to be stated. Actual"
            + " report:\n" + report,
            report.substring(sectionIndex).contains("None: no shell process has exited yet"));
    }

    @Test
    public void theShellExitTallySitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(new DiagnosticsShellExits(Collections.singletonList(
            new DiagnosticsShellExitCount(255, 18))));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the tally has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " tally that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
