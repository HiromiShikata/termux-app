package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionReconnectReason;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Three different rules plan a reconnect and they mean very different things to the owner. A session
 * whose shell process has exited has genuinely lost its connection and must come back. A session
 * whose input no longer reaches the program reading the terminal has also lost it. A session that has
 * merely produced no output for longer than the staleness threshold may be perfectly healthy and
 * idle, and replacing it discards input and loses the call it was holding. A single total cannot say
 * which rule is responsible for the reconnects being observed, so the behaviour cannot be tuned.
 */
public class SessionReconnectCostPerReasonInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final long ONE_MILLISECOND_IN_NANOS = 1000000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportWith(SessionReconnectCostCounter counter) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            DiagnosticsSessionReconnectCost.of(counter),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList()),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(), DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED, DiagnosticsAppProcessPopulation.UNMEASURED);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static String reconnectSectionOf(String renderedReport) {
        int reconnectSectionIndex = renderedReport.indexOf("Dead session reconnect on the main thread");
        Assert.assertTrue("the reconnect section must exist before its per-reason lines can be read."
            + " Actual report:\n" + renderedReport, reconnectSectionIndex >= 0);
        int nextSectionIndex = renderedReport.indexOf("\nSessions\n", reconnectSectionIndex);
        return nextSectionIndex < 0
            ? renderedReport.substring(reconnectSectionIndex)
            : renderedReport.substring(reconnectSectionIndex, nextSectionIndex);
    }

    @Test
    public void everyReasonThatPlannedAReconnectIsCountedAndCostedSeparatelyInTheReport() {
        SessionReconnectCostCounter counter = new SessionReconnectCostCounter();
        counter.record(SessionReconnectReason.SHELL_PROCESS_EXITED, 7 * ONE_MILLISECOND_IN_NANOS, 0);
        counter.record(SessionReconnectReason.SHELL_PROCESS_EXITED, 5 * ONE_MILLISECOND_IN_NANOS, 1);
        counter.record(SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD,
            40 * ONE_MILLISECOND_IN_NANOS, 3);
        counter.record(SessionReconnectReason.INPUT_NO_LONGER_REACHES_THE_PROGRAM,
            11 * ONE_MILLISECOND_IN_NANOS, 2);

        String section = reconnectSectionOf(renderedReportWith(counter));

        Assert.assertTrue("a reconnect planned because the shell process exited has to be countable on"
                + " its own. Actual section:\n" + section,
            section.contains("Shell process exited\n      Count: 2\n      Total: 12 ms"));
        Assert.assertTrue("replacing a session that is merely idle discards the owner's input, so the"
                + " share of reconnects planned by the staleness rule is the figure that decides whether"
                + " that rule is worth keeping. Actual section:\n" + section,
            section.contains("Silent for longer than the staleness threshold\n      Count: 1\n"
                + "      Total: 40 ms"));
        Assert.assertTrue("a session whose input no longer reaches the program has genuinely lost its"
                + " connection and is counted apart from the staleness rule. Actual section:\n" + section,
            section.contains("Input no longer reaches the program\n      Count: 1\n      Total: 11 ms"));
    }

    @Test
    public void aReasonThatPlannedNoReconnectIsNotListedSoTheDominantReasonStandsOut() {
        SessionReconnectCostCounter counter = new SessionReconnectCostCounter();
        counter.record(SessionReconnectReason.SHELL_PROCESS_EXITED, ONE_MILLISECOND_IN_NANOS, 0);

        String section = reconnectSectionOf(renderedReportWith(counter));

        Assert.assertTrue("the reason that did plan a reconnect must be named. Actual section:\n"
            + section, section.contains("Shell process exited"));
        Assert.assertFalse("listing reasons that never fired pads the section and hides which rule is"
                + " actually responsible. Actual section:\n" + section,
            section.contains("Silent for longer than the staleness threshold"));
    }

    @Test
    public void theSlowestReconnectOfEachReasonIsReportedSoOneHeavyRuleIsNotHiddenInATotal() {
        SessionReconnectCostCounter counter = new SessionReconnectCostCounter();
        counter.record(SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD,
            9 * ONE_MILLISECOND_IN_NANOS, 0);
        counter.record(SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD,
            120 * ONE_MILLISECOND_IN_NANOS, 4);

        String section = reconnectSectionOf(renderedReportWith(counter));

        Assert.assertTrue("a rule whose reconnects are individually slow reads the same as a rule that"
                + " simply fires often once both are averaged into a total. Actual section:\n" + section,
            section.contains("Silent for longer than the staleness threshold\n      Count: 2\n"
                + "      Total: 129 ms\n      Max: 120 ms"));
    }
}
