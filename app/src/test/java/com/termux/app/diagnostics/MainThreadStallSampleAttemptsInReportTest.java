package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class MainThreadStallSampleAttemptsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Stalls over ";

    private static String renderedReportOf(DiagnosticsMainThreadStalls stalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            stalls,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportSeparatesStallsTheWatchdogNeverReachedFromStallsTheRuntimeWouldNotDescribe() {
        String report = renderedReportOf(new DiagnosticsMainThreadStalls(80L, 61L, 1033L, "",
            Collections.<MainThreadStallHotPath>emptyList(), 7L, 4L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the stall section has to be present. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("a stall reported as not sampled is either one the watchdog never woke up for or"
                + " one the runtime returned no frames for, and only the attempt count tells the two apart."
                + " Actual report:\n" + report,
            section.contains("Stack sample attempts: 7"));
        Assert.assertTrue("without the empty answer count, raising the sampling rate cannot be told apart"
                + " from a runtime that will not describe the main thread at all. Actual report:\n" + report,
            section.contains("of which the runtime returned no frames: 4"));
    }

    @Test
    public void aRunThatNeverAttemptedASampleSaysZeroRatherThanOmittingTheLine() {
        String report = renderedReportOf(new DiagnosticsMainThreadStalls(80L, 61L, 1033L, "",
            Collections.<MainThreadStallHotPath>emptyList(), 0L, 0L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the stall section has to be present. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("a measured zero says the watchdog never reached any of the stalls, which is a"
                + " finding about the watchdog rather than about the runtime. Actual report:\n" + report,
            section.contains("Stack sample attempts: 0"));
    }

    @Test
    public void theSampleAttemptCountsSitInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(new DiagnosticsMainThreadStalls(80L, 61L, 1033L, "",
            Collections.<MainThreadStallHotPath>emptyList(), 7L, 4L));

        int lineIndex = report.indexOf("Stack sample attempts: ");
        Assert.assertTrue("the line has to be present for its position to matter. Actual report:\n" + report,
            lineIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only the"
                + " first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a line that"
                + " falls outside that window cannot be read at all. It currently begins at character "
                + lineIndex + ". Actual report:\n" + report,
            lineIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
