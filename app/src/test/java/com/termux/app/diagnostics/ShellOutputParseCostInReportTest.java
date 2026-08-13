package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ShellOutputParseCostInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsWorkCostLine shellOutputParseCost) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            shellOutputParseCost, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportSaysHowMuchOfTheMainThreadWentIntoParsingShellOutput() {
        String report = renderedReportOf(new DiagnosticsWorkCostLine(58000, 21000, 47, 9000));

        Assert.assertTrue("every session's output is parsed on the main thread, so the share of the"
                + " main thread that goes into it has to be in the report the owner sends\n" + report,
            report.contains("  Shell output parse\n"
                + "    Count: 58000\n"
                + "    Total: 21000 ms\n"
                + "    Max: 47 ms\n"
                + "    Transcript rows at max: 9000\n"));
    }

    @Test
    public void aReportTakenBeforeAnySessionProducedOutputSaysSoRatherThanShowingAMadeUpMaximum() {
        String report = renderedReportOf(NO_WORK_COST);

        Assert.assertTrue("a report with no parsed output behind it must not read as a fast parse\n" + report,
            report.contains("  Shell output parse\n"
                + "    Count: 0\n"
                + "    Total: 0 ms\n"
                + "    Max: n/a\n"));
    }
}
