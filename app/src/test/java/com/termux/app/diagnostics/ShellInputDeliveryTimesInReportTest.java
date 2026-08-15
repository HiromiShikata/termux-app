package com.termux.app.diagnostics;

import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ShellInputDeliveryTimesInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final long ACCEPTED_MILLIS = REPORT_MILLIS - 1000L;

    private static final long DISCARDED_MILLIS = REPORT_MILLIS - 2000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsShellInputDelivery shellInputDelivery) {
        DiagnosticsSessionLine sessionLine = new DiagnosticsSessionLine("host-a", true, 12, true, 0, 98,
            DiagnosticsSessionListDisplay.DISPLAYED, shellInputDelivery,
            new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.GRAY),
            DiagnosticsScrollGestureRouting.ofEmulatorState(true, true),
            DiagnosticsSessionListAbsence.presentInTheList());
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            1, 1, 32, Collections.singletonList(sessionLine),
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
            NO_WORK_COST, NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE,
            DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aDiscardCarriesTheTimeItHappenedSoItCanBeTiedToTheGestureJustAttempted() {
        String report = renderedReportOf(new DiagnosticsShellInputDelivery(88, 88, 440, true, "",
            ACCEPTED_MILLIS, DISCARDED_MILLIS));

        Assert.assertTrue("a cumulative discard count with no time cannot say whether the loss happened"
                + " during the gesture the owner just made or hours earlier\n" + report,
            report.contains("discarded before the queue 440B at 2026-07-05T01:59:58Z\n"));
    }

    @Test
    public void anAcceptanceCarriesTheTimeItHappenedSoBytesThatNeverLeftTheViewCanBeTold() {
        String report = renderedReportOf(new DiagnosticsShellInputDelivery(88, 88, 440, true, "",
            ACCEPTED_MILLIS, DISCARDED_MILLIS));

        Assert.assertTrue("a session that accepted nothing at the moment of the gesture never received"
                + " the bytes at all, which is a different failure from accepting and being ignored\n"
                + report,
            report.contains("shell input: accepted 88B at 2026-07-05T01:59:59Z,"));
    }

    @Test
    public void aSessionThatDiscardedNothingReportsNoDiscardTimeRatherThanAStandIn() {
        String report = renderedReportOf(new DiagnosticsShellInputDelivery(88, 88, 0, true, "",
            ACCEPTED_MILLIS, null));

        Assert.assertTrue("printing any time for a discard that never happened would name a loss the"
                + " owner never suffered\n" + report,
            report.contains("discarded before the queue 0B\n"));
    }

    @Test
    public void aSessionThatAcceptedNothingReportsNoAcceptanceTimeRatherThanAStandIn() {
        String report = renderedReportOf(new DiagnosticsShellInputDelivery(0, 0, 440, true, "",
            null, DISCARDED_MILLIS));

        Assert.assertTrue("a session that has accepted nothing has no acceptance to time, and a"
                + " substituted value would read as input that was delivered\n" + report,
            report.contains("shell input: accepted 0B, written to the shell 0B"));
    }

    @Test
    public void theTimesSitInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(new DiagnosticsShellInputDelivery(88, 88, 440, true, "",
            ACCEPTED_MILLIS, DISCARDED_MILLIS));

        Assert.assertTrue("a report longer than the paste limit loses its tail, and these times sit in"
                + " the session lines near the end of it\n" + report.length(),
            report.length() <= DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
        Assert.assertTrue("the times have to survive the truncation that keeps the report pasteable\n"
                + report, report.contains("at 2026-07-05T01:59:58Z\n"));
    }
}
