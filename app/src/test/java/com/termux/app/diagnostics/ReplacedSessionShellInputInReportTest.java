package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * The record of how much typed input reached the shell lives on the terminal session, and the
 * automatic reconnect replaces a dead session rather than restarting it, so every figure returns to
 * zero the moment the application recovers on its own. A user who takes a report after that recovery
 * cannot tell it apart from a report taken on a healthy application, which is the state they are
 * normally in by the time they are able to take one at all.
 */
public class ReplacedSessionShellInputInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static String renderedReport() {
        return renderedReportOf(new DiagnosticsReplacedSessionShellInput(0, 0L, "", "", ""));
    }

    private static String renderedReportOf(DiagnosticsReplacedSessionShellInput replacedSessionShellInput) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0),
            replacedSessionShellInput,
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList()),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch());
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void inputStuckOnASessionThatHasSinceBeenReplacedIsStillAccountedForInTheReport() {
        String report = renderedReport();

        int replacedSectionIndex = report.indexOf("Input stuck on sessions replaced since the app started");
        Assert.assertTrue("the only record of input that never reached the shell lives on the terminal"
                + " session, and the automatic reconnect builds a replacement rather than restarting"
                + " the dead one, so an episode is erased as soon as the application recovers. A report"
                + " that never accounts for the sessions it replaced therefore reads identically"
                + " whether nothing went wrong or everything did. Actual report:\n" + report,
            replacedSectionIndex >= 0);

        Assert.assertTrue("nothing recorded must read as measured-and-none rather than as a missing"
                + " measurement, otherwise a reader cannot tell a healthy application from one whose"
                + " history was discarded. Actual report:\n" + report,
            report.substring(replacedSectionIndex).contains("Sessions replaced with input still undelivered: 0"));
    }

    @Test
    public void theAccountOfReplacedSessionsSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReport();

        int replacedSectionIndex = report.indexOf("Input stuck on sessions replaced since the app started");
        Assert.assertTrue("the account has to be present for its position to matter. Actual report:\n"
            + report, replacedSectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so an"
                + " account that falls outside that window cannot be read at all. It currently begins"
                + " at character " + replacedSectionIndex + ". Actual report:\n" + report,
            replacedSectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }

    @Test
    public void theWorstSessionAndTheLastWriterStopSurviveTheReplacementThatEndedTheEpisode() {
        String report = renderedReportOf(new DiagnosticsReplacedSessionShellInput(3, 3096L,
            "host-stuck", "host-stuck", "broken pipe"));

        int replacedSectionIndex = report.indexOf("Input stuck on sessions replaced since the app started");
        Assert.assertTrue("the account has to be present for its content to matter. Actual report:\n"
            + report, replacedSectionIndex >= 0);
        String section = report.substring(replacedSectionIndex);
        Assert.assertTrue("the number of replaced sessions that still had input outstanding is what tells"
            + " a reader an episode happened at all. Actual report:\n" + report,
            section.contains("Sessions replaced with input still undelivered: 3"));
        Assert.assertTrue("the size of the worst loss and the session it happened on are what make the"
            + " episode actionable. Actual report:\n" + report,
            section.contains("Most left unwritten: 3096B on host-stuck"));
        Assert.assertTrue("why the writer stopped is the only thing distinguishing a shell that died from"
            + " one that merely fell behind. Actual report:\n" + report,
            section.contains("Last writer stop: host-stuck (broken pipe)"));
    }

    @Test
    public void aReplacedSessionWhoseWriterWasStillRunningIsReportedWithoutAWriterStopLine() {
        String report = renderedReportOf(new DiagnosticsReplacedSessionShellInput(1, 512L,
            "host-slow", "", ""));

        int replacedSectionIndex = report.indexOf("Input stuck on sessions replaced since the app started");
        String section = report.substring(replacedSectionIndex);
        Assert.assertTrue("input left unwritten by a session whose writer never stopped is still a loss"
            + " and must be reported. Actual report:\n" + report,
            section.contains("Most left unwritten: 512B on host-slow"));
        Assert.assertFalse("no writer ever stopped, so naming a writer stop would invent an event that"
            + " did not happen. Actual report:\n" + report, section.contains("Last writer stop:"));
    }
}
