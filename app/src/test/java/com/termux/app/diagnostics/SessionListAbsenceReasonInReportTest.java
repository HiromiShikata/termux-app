package com.termux.app.diagnostics;

import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class SessionListAbsenceReasonInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsSessionListDisplay listDisplay) {
        DiagnosticsSessionLine sessionLine = new DiagnosticsSessionLine("host-a", true, 12, true, 0, 98,
            listDisplay,
            new DiagnosticsShellInputDelivery(88, 88, 0, true, ""),
            new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.GRAY),
            DiagnosticsScrollGestureRouting.ofEmulatorState(false, false));
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            1, 0, 32, Collections.singletonList(sessionLine),
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
            NO_WORK_COST, NO_WORK_COST);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aSessionTheListLeavesOutStatesWhyTheListLeftItOut() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED);

        Assert.assertTrue("a session left out because the owner collapsed its project group is the owner's"
                + " own choice, while a session left out because the list built no row for it keeps a"
                + " process and a cap slot alive out of sight, and the report reads identically for both"
                + " until the reason is stated\n" + report,
            report.contains("      not displayed because "));
    }
}
