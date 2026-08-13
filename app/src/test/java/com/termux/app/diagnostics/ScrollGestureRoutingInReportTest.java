package com.termux.app.diagnostics;

import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ScrollGestureRoutingInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final DiagnosticsSessionListAbsence PRESENT_IN_THE_LIST =
        DiagnosticsSessionListAbsence.presentInTheList();

    private static String renderedReportOf(DiagnosticsScrollGestureRouting scrollGestureRouting) {
        DiagnosticsSessionLine sessionLine = new DiagnosticsSessionLine("host-a", true, 12, true, 0, 98,
            DiagnosticsSessionListDisplay.DISPLAYED,
            new DiagnosticsShellInputDelivery(88, 88, 0, true, ""),
            new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.GRAY),
            scrollGestureRouting, PRESENT_IN_THE_LIST);
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
            NO_WORK_COST, NO_WORK_COST);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aSessionWhoseGestureReachesTheViewSaysSoUnderItsOwnLine() {
        String report = renderedReportOf(DiagnosticsScrollGestureRouting.ofEmulatorState(false, false));

        Assert.assertTrue("without this the owner cannot tell a session that scrolls from one that only"
                + " looks like it should\n" + report,
            report.contains("  - host-a | alive | last activity: 12s ago | transcript rows: 0"
                + " | columns: 98 | displayed in list\n"
                + "      a scroll gesture goes to the view's own scrollback"
                + " (mouse tracking off, alternate screen off)\n"));
    }

    @Test
    public void aSessionOnTheAlternateScreenSaysTheGestureLeavesAsArrowKeys() {
        String report = renderedReportOf(DiagnosticsScrollGestureRouting.ofEmulatorState(false, true));

        Assert.assertTrue("a gesture turned into arrow keys moves nothing when the program on the other"
                + " end ignores them, and that is indistinguishable from a slow view\n" + report,
            report.contains("      a scroll gesture goes to the shell as arrow keys"
                + " (mouse tracking off, alternate screen on)\n"));
    }

    @Test
    public void aSessionLeftWithMouseTrackingOnSaysTheGestureLeavesAsAMouseWheel() {
        String report = renderedReportOf(DiagnosticsScrollGestureRouting.ofEmulatorState(true, true));

        Assert.assertTrue("nothing clears mouse tracking when a program dies with it on, so the report"
                + " has to show a session stuck in that state\n" + report,
            report.contains("      a scroll gesture goes to the shell as a mouse wheel"
                + " (mouse tracking on, alternate screen on)\n"));
    }

    @Test
    public void aSessionWithNoEmulatorSaysSoRatherThanClaimingItScrollsTheView() {
        String report = renderedReportOf(DiagnosticsScrollGestureRouting.withoutAnEmulator());

        Assert.assertTrue("a dead session has no emulator to route a gesture at all, and reporting the"
                + " default routing there would be a made-up answer\n" + report,
            report.contains("      a scroll gesture goes to no emulator\n"));
    }

    @Test
    public void theListDisplayStateStaysTheLastFieldSoItCanStillBeReadOffTheEndOfTheLine() {
        String report = renderedReportOf(DiagnosticsScrollGestureRouting.ofEmulatorState(false, true));

        String sessionLine = null;
        for (String line : report.split("\n")) {
            if (line.startsWith("  - ")) {
                sessionLine = line;
                break;
            }
        }

        Assert.assertNotNull("the rendered report has to carry a session line at all\n" + report,
            sessionLine);
        Assert.assertEquals("the display state is read off the end of the session line, so any field"
                + " placed after it makes that reading return something else entirely",
            DiagnosticsSessionListDisplay.DISPLAYED.getReportLabel(),
            sessionLine.substring(sessionLine.lastIndexOf(" | ") + " | ".length()));
    }
}
