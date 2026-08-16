package com.termux.app.diagnostics;

import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ScrollAnswerInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final long A_MINUTE_BEFORE_THE_REPORT_MILLIS = 1783216740000L;

    private static final long HALF_A_MINUTE_BEFORE_THE_REPORT_MILLIS = 1783216770000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsScrollAnswer scrollAnswer) {
        DiagnosticsSessionLine sessionLine = new DiagnosticsSessionLine("host-a", true, 12, true, 0, 98,
            DiagnosticsSessionListDisplay.DISPLAYED,
            new DiagnosticsShellInputDelivery(88, 88, 0, true, "", null, null),
            new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.GRAY),
            DiagnosticsScrollGestureRouting.ofEmulatorState(true, true),
            DiagnosticsSessionListAbsence.presentInTheList(),
            scrollAnswer);
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
    public void aSessionAnsweringEveryScrollSaysSoUnderItsOwnLine() {
        String report = renderedReportOf(new DiagnosticsScrollAnswer(12L, 12L, null,
            HALF_A_MINUTE_BEFORE_THE_REPORT_MILLIS));

        Assert.assertTrue("with the alternate screen active the terminal repaints only when the program"
                + " produces output, so a session answering every scroll is the only evidence that"
                + " scrolling works there at all\n" + report,
            report.contains("      scroll answered by the program: 12 of 12,"
                + " last answered at 2026-07-05T01:59:30Z\n"));
    }

    @Test
    public void aSessionThatStoppedAnsweringCarriesTheTimeTheScrollItOwesWasSent() {
        String report = renderedReportOf(new DiagnosticsScrollAnswer(12L, 11L,
            A_MINUTE_BEFORE_THE_REPORT_MILLIS, HALF_A_MINUTE_BEFORE_THE_REPORT_MILLIS));

        Assert.assertTrue("a session that answered eleven scrolls and owes the twelfth is the failure"
                + " this measures, and the moment it was sent is what dates the failure against"
                + " everything else in the reading\n" + report,
            report.contains("      scroll answered by the program: 11 of 12,"
                + " one waiting since 2026-07-05T01:59:00Z,"
                + " last answered at 2026-07-05T01:59:30Z\n"));
    }

    @Test
    public void aSessionNobodyHasScrolledSaysSoRatherThanReadingAsAFailureToAnswer() {
        String report = renderedReportOf(new DiagnosticsScrollAnswer(0L, 0L, null, null));

        Assert.assertTrue("nearly every session in a reading has never been scrolled, and rendering"
                + " those as nought of nought would put an apparent failure on all of them and bury"
                + " the one session that matters\n" + report,
            report.contains("      scroll answered by the program: no scroll has been sent to it"
                + " yet\n"));
    }

    @Test
    public void aSessionThatHasNeverAnsweredAScrollCarriesNoTimeOfALastAnswer() {
        String report = renderedReportOf(new DiagnosticsScrollAnswer(1L, 0L,
            A_MINUTE_BEFORE_THE_REPORT_MILLIS, null));

        Assert.assertTrue("a session that has never answered one scroll is a program that does not act"
                + " on a wheel report at all rather than one that stopped, and inventing a time of a"
                + " last answer there would hide that distinction\n" + report,
            report.contains("      scroll answered by the program: 0 of 1,"
                + " one waiting since 2026-07-05T01:59:00Z\n"));
    }

    @Test
    public void theAnswerLineSitsWithTheRoutingLineThatSaysWhereTheScrollWent() {
        String report = renderedReportOf(new DiagnosticsScrollAnswer(3L, 3L, null,
            HALF_A_MINUTE_BEFORE_THE_REPORT_MILLIS));

        Assert.assertTrue("where a scroll goes and whether it came back are one question, and a reader"
                + " who has to hunt for the second half in another part of the reading will not"
                + " connect them\n" + report,
            report.contains("      a scroll gesture goes to the shell as a mouse wheel"
                + " (mouse tracking on, alternate screen on)\n"
                + "      scroll answered by the program: 3 of 3,"
                + " last answered at 2026-07-05T01:59:30Z\n"));
    }
}
