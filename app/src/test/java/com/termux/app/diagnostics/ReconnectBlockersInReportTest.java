package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionReconnectBlockerCensus;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReconnectBlockersInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING =
        "Why the sessions on screen were not reconnected at the last scan";

    private static String renderedReportOf(SessionReconnectBlockerCensus census) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST, NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE,
            DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE, DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED, census);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void aRunWithNoBackgroundScanYetSaysSoRatherThanLeavingTheSectionOut() {
        String report = renderedReportOf(SessionReconnectBlockerCensus.NOT_TAKEN);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an application whose reconnect decisions were"
                + " never measured, which is the state a reader has to be able to rule out. Actual"
                + " report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no scan having run is itself the answer to why nothing was reconnected."
                + " Actual report:\n" + report,
            report.substring(sectionIndex).contains("None: no background reconnect scan has run yet"));
    }

    @Test
    public void theBackoffThatHeldADeadSessionBackIsNamedWithTheWaitItStillHad() {
        String report = renderedReportOf(SessionReconnectBlockerCensus.of(Arrays.asList(
            new SessionReconnectBlockerCensus.ConsideredSession(false, false, false, 0L, false, false,
                252_000L, 0L, false)), REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        String section = report.substring(sectionIndex);
        Assert.assertTrue("the count of sessions whose shell is gone is what the owner sees as rows that"
            + " do nothing. Actual report:\n" + report, section.contains("  Shell gone: 1\n"));
        Assert.assertTrue("the owner reports a visible session left unreconnected for over a minute, and"
                + " the remaining wait is the only number that says whether this backoff is the reason."
                + " Actual report:\n" + report,
            section.contains("    inside the exit backoff: 1, longest wait left 0h 4m 12s\n"));
    }

    @Test
    public void theSessionOnScreenThatTheSilentPathSkipsIsNamedInTheReport() {
        String report = renderedReportOf(SessionReconnectBlockerCensus.of(Arrays.asList(
            new SessionReconnectBlockerCensus.ConsideredSession(true, true, false, 0L, false, true, 0L,
                0L, false)), REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        String section = report.substring(sectionIndex);
        Assert.assertTrue("the session the owner is looking at is deliberately excluded from the"
                + " silent-session reconnect, and a reader who cannot see that exclusion counted will"
                + " look for a defect that is not there. Actual report:\n" + report,
            section.contains("    the one on screen, which this scan skips: 1\n"));
    }

    @Test
    public void aSessionHeldByAReconnectThatNeverFinishedIsReportedWithHowLongItHasBeenHeld() {
        String report = renderedReportOf(SessionReconnectBlockerCensus.of(Arrays.asList(
            new SessionReconnectBlockerCensus.ConsideredSession(false, false, true, 201_000L, true,
                false, 0L, 0L, false)), REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        String section = report.substring(sectionIndex);
        Assert.assertTrue("a reconnect mark that outlives the reconnect excludes the session from every"
                + " later attempt, and its age is what separates that from an attempt in flight. Actual"
                + " report:\n" + report,
            section.contains("    still marked reconnecting: 1, longest 0h 3m 21s\n"));
        Assert.assertTrue("a session the app already gave up on is the row that shows the reload button,"
                + " and the owner asks about exactly those rows. Actual report:\n" + report,
            section.contains("    reported failed to reconnect: 1\n"));
    }

    @Test
    public void theReconnectBlockersSitInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(SessionReconnectBlockerCensus.of(Arrays.asList(
            new SessionReconnectBlockerCensus.ConsideredSession(false, false, false, 0L, false, false,
                60_000L, 0L, false)), REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " section that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
        Assert.assertTrue("the breakdown is the whole value of the section, and it is the part the"
                + " character budget can drop. Actual report:\n" + report,
            report.substring(sectionIndex).contains("longest wait left 0h 1m 0s"));
    }
}
