package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TerminalDrawCostInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsWorkCostLine terminalDrawCost) {
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
            terminalDrawCost,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportSaysHowMuchOfTheMainThreadWentIntoDrawingTheTerminal() {
        String report = renderedReportOf(new DiagnosticsWorkCostLine(4200, 9100, 340, 12000));

        Assert.assertTrue("the owner reports that scrolling becomes impossible, so what the terminal"
                + " spends on drawing has to be in the report he sends\n" + report,
            report.contains("  Terminal draw\n"
                + "    Count: 4200\n"
                + "    Total: 9100 ms\n"
                + "    Max: 340 ms\n"
                + "    Transcript rows at max: 12000\n"));
    }

    @Test
    public void aReportTakenBeforeTheTerminalEverDrewSaysSoRatherThanShowingAMadeUpMaximum() {
        String report = renderedReportOf(NO_WORK_COST);

        Assert.assertTrue("a report with no draw behind it must not read as a fast draw\n" + report,
            report.contains("  Terminal draw\n"
                + "    Count: 0\n"
                + "    Total: 0 ms\n"
                + "    Max: n/a\n"));
    }
}
