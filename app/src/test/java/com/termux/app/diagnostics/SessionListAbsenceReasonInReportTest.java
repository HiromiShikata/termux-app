package com.termux.app.diagnostics;

import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SessionListAbsenceReasonInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReportOf(DiagnosticsSessionListDisplay listDisplay,
                                           DiagnosticsSessionListAbsence listAbsence) {
        DiagnosticsSessionLine sessionLine = new DiagnosticsSessionLine("host-a", true, 12, true, 0, 98,
            listDisplay,
            new DiagnosticsShellInputDelivery(88, 88, 0, true, ""),
            new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.GRAY),
            DiagnosticsScrollGestureRouting.ofEmulatorState(false, false), listAbsence);
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
            NO_WORK_COST, NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsSessionListAbsence absenceOf(java.util.List<String> collapsedProjectSessionNames,
                                                           java.util.List<String> hiddenSessionNames) {
        return DiagnosticsSessionListAbsence.ofListState(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            "host-a", collapsedProjectSessionNames, hiddenSessionNames);
    }

    @Test
    public void aSessionLeftOutBecauseItsProjectGroupIsCollapsedSaysSo() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            absenceOf(Arrays.asList("host-a"), Collections.<String>emptyList()));

        Assert.assertTrue("a session inside a group the owner collapsed is the owner's own choice and"
                + " reading it as a session the list lost sends the search after a defect that is not"
                + " there\n" + report,
            report.contains("      not displayed because the project group holding it is collapsed\n"));
    }

    @Test
    public void aSessionLeftOutBecauseItsNameIsHiddenSaysSo() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            absenceOf(Collections.<String>emptyList(), Arrays.asList("host-a")));

        Assert.assertTrue("a hidden session is deliberately out of the list and costs nothing to explain\n"
                + report,
            report.contains("      not displayed because its name is hidden and hidden sessions are being"
                + " hidden\n"));
    }

    @Test
    public void aHiddenSessionInsideACollapsedGroupSaysItIsHiddenRatherThanCollapsed() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            absenceOf(Arrays.asList("host-a"), Arrays.asList("host-a")));

        Assert.assertTrue("a hidden name is excluded from the count of sessions held against the cap"
                + " while a collapsed one is not, so calling this session collapsed would put it in a"
                + " gap it cannot be part of\n" + report,
            report.contains("      not displayed because its name is hidden and hidden sessions are being"
                + " hidden\n"));
    }

    @Test
    public void aSessionTheListBuiltNoRowForSaysSo() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            absenceOf(Collections.<String>emptyList(), Collections.<String>emptyList()));

        Assert.assertTrue("this is the reading that matters, because such a session holds a process and a"
                + " slot against the session cap while the owner cannot reach it at all\n" + report,
            report.contains("      not displayed because the list built no row for it\n"));
    }

    @Test
    public void aSessionTheListShowsCarriesNoAbsenceLineAtAll() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.DISPLAYED,
            DiagnosticsSessionListAbsence.presentInTheList());

        Assert.assertFalse("a line explaining an absence that did not happen is noise in a report already"
                + " long enough to be cut off\n" + report,
            report.contains("not displayed because"));
    }

    @Test
    public void aReadingTakenBeforeTheListWasBuiltClaimsNoReason() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_KNOWN,
            DiagnosticsSessionListAbsence.ofListState(DiagnosticsSessionListDisplay.NOT_KNOWN, "host-a",
                Collections.<String>emptyList(), Collections.<String>emptyList()));

        Assert.assertFalse("nothing is known about a list that has not been built, and naming a reason"
                + " there would be a made-up answer\n" + report,
            report.contains("not displayed because"));
    }

    @Test
    public void theSessionLineItselfStillEndsWithTheDisplayState() {
        String report = renderedReportOf(DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            absenceOf(Arrays.asList("host-a"), Collections.<String>emptyList()));

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
            DiagnosticsSessionListDisplay.NOT_DISPLAYED.getReportLabel(),
            sessionLine.substring(sessionLine.lastIndexOf(" | ") + " | ".length()));
    }
}
