package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Reconnecting a dead session runs on the main thread, and a bulk reconnect runs one session per
 * main-thread message. Work that stays under the stall watchdog's threshold but repeats once per
 * session leaves the interface unresponsive for the whole run while the report shows few or no
 * stalls, so the reconnect cost has to be measured in its own right rather than inferred from the
 * stall section.
 */
public class SessionReconnectCostInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportWithNothingRecorded() {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList()),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()));
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theMainThreadCostOfReconnectingDeadSessionsIsNamedInTheReport() {
        String renderedReport = renderedReportWithNothingRecorded();

        int reconnectSectionIndex = renderedReport.indexOf("Dead session reconnect on the main thread");
        Assert.assertTrue("a bulk reconnect spends main-thread time once per session, and no other"
                + " section of the report attributes that time, so a report that never names it cannot"
                + " answer whether reconnecting is what makes the interface heavy. Actual report:\n"
                + renderedReport,
            reconnectSectionIndex >= 0);

        int backgroundScanSectionIndex = renderedReport.indexOf("Background output tag scan");
        Assert.assertTrue("the reconnect cost belongs beside the other named main-thread costs so the"
                + " three are read against each other. Actual report:\n" + renderedReport,
            backgroundScanSectionIndex >= 0 && reconnectSectionIndex > backgroundScanSectionIndex);

        Assert.assertTrue("a reconnect count of zero must read as measured-and-none rather than as a"
                + " missing measurement. Actual report:\n" + renderedReport,
            renderedReport.substring(reconnectSectionIndex).contains("Count: 0"));
    }
}
