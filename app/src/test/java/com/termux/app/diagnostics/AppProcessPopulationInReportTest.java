package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AppProcessPopulationInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Processes this app is running";

    private static String renderedReportOf(DiagnosticsAppProcessPopulation appProcessPopulation) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList()),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            appProcessPopulation);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportStatesHowManyProcessesTheAppIsRunningAndWhichCommandsTheyAre() {
        String report = renderedReportOf(DiagnosticsAppProcessPopulation.measured(49, Arrays.asList(
            new DiagnosticsProcessCommandCount("ssh", 24),
            new DiagnosticsProcessCommandCount("sh", 24),
            new DiagnosticsProcessCommandCount("com.termux", 1))));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("Android kills the app's forked processes once they pass a ceiling, so a report"
                + " that never states how many processes the app is running cannot say whether the app is"
                + " above that ceiling. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the total is the number compared against the ceiling. Actual report:\n" + report,
            section.contains("Total: 49"));
        Assert.assertTrue("the per-command breakdown is what shows whether a session costs one process or"
                + " several, which decides what has to be removed. Actual report:\n" + report,
            section.contains("ssh: 24"));
        Assert.assertTrue("the wrapper shell is the process a fix would remove, so it has to be visible"
            + " separately from the connection itself. Actual report:\n" + report, section.contains("sh: 24"));
    }

    @Test
    public void aPopulationThatHasNotBeenReadYetSaysSoRatherThanReportingZero() {
        String report = renderedReportOf(DiagnosticsAppProcessPopulation.UNMEASURED);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present even before the first read, so its absence is"
            + " never mistaken for a report from an older build. Actual report:\n" + report, sectionIndex >= 0);
        Assert.assertTrue("a population that was never read must not render as a total of zero, because"
                + " zero would read as the app being far below the ceiling. Actual report:\n" + report,
            report.substring(sectionIndex).contains("Total: not measured yet"));
    }

    @Test
    public void aReadThatFailedNamesTheFailureRatherThanReportingZero() {
        String report = renderedReportOf(
            DiagnosticsAppProcessPopulation.readFailed("the process table could not be listed"));

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("a failed read rendered as zero processes would contradict the shells being"
                + " killed for exceeding the ceiling. Actual report:\n" + report,
            section.contains("Total: not readable"));
        Assert.assertTrue("the reason the read failed is what makes the missing number actionable."
                + " Actual report:\n" + report,
            section.contains("Read failed: the process table could not be listed"));
    }

    @Test
    public void theSectionSurvivesBeingPastedBecauseItSitsInsideThePasteLimit() {
        String report = renderedReportOf(DiagnosticsAppProcessPopulation.measured(49, Arrays.asList(
            new DiagnosticsProcessCommandCount("ssh", 24),
            new DiagnosticsProcessCommandCount("sh", 24),
            new DiagnosticsProcessCommandCount("com.termux", 1))));

        int sectionEndIndex = report.indexOf(SECTION_HEADING) + SECTION_HEADING.length();
        Assert.assertTrue("a section past the paste limit is cut off when the owner pastes the report,"
                + " which loses exactly the evidence the section exists to carry. Actual length: "
                + report.length(),
            sectionEndIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
