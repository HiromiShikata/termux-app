package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class SessionCreationPathInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Sessions created since the app started";

    private static String renderedReportOf(DiagnosticsSessionCreationPaths creationPaths) {
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
            NO_WORK_COST, NO_WORK_COST, creationPaths, DiagnosticsActivityWindows.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsSessionCreationPaths countedPaths(SessionCreationPath... paths) {
        SessionCreationPathCounter counter = new SessionCreationPathCounter();
        for (SessionCreationPath path : paths) {
            counter.record(path);
        }
        return DiagnosticsSessionCreationPaths.of(counter);
    }

    @Test
    public void aSessionCreatedOutsideThePacedReconnectIsCountedUnderItsOwnPath() {
        String report = renderedReportOf(countedPaths(
            SessionCreationPath.RECONNECT_OF_A_FINISHED_SESSION_IN_PLACE,
            SessionCreationPath.RECONNECT_OF_A_FINISHED_SESSION_IN_PLACE,
            SessionCreationPath.RECONNECT_OF_A_FINISHED_SESSION_IN_PLACE));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the reconnect cost counter is written only by the reconnect pacer, so a"
                + " session that appears through any other path raises the count held against the"
                + " session cap and takes a shell process with it while every number already in the"
                + " report stays where it was. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("naming the path is the whole point: a reader has to be able to say which code"
                + " created the sessions that appeared. Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("  Reconnect of a finished session in place: 3\n"));
    }

    @Test
    public void theTotalStatesHowMuchOfTheSessionPopulationMovementIsAccountedFor() {
        String report = renderedReportOf(countedPaths(
            SessionCreationPath.RESTORE_OF_A_PERSISTED_SESSION,
            SessionCreationPath.NEW_AUTOSSH_SESSION));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its total can be read. Actual report:\n"
            + report, sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("without a total a reader cannot tell whether the listed paths account for the"
            + " sessions that appeared. Actual report:\n" + report, section.contains("  Total: 2\n"));
        Assert.assertTrue("each path that created a session has to be named. Actual report:\n" + report,
            section.contains("  Restore of a persisted session: 1\n"));
        Assert.assertTrue("each path that created a session has to be named. Actual report:\n" + report,
            section.contains("  New autossh session: 1\n"));
    }

    @Test
    public void aPathThatCreatedNothingIsLeftOutRatherThanPrintedAsZero() {
        String report = renderedReportOf(countedPaths(SessionCreationPath.KILL_OF_A_HOST_SESSION));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertFalse("the report is cut at a paste limit and every line spent on a path that did"
                + " nothing is a line taken from the stall attribution that is already being"
                + " truncated. Actual report:\n" + report,
            report.substring(sectionIndex).contains("Reset of a host session"));
    }

    @Test
    public void aRunInWhichNoSessionWasCreatedIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsSessionCreationPaths.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is the state a"
                + " reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no session having been created is itself a finding, because it means the"
                + " sessions on screen are the ones the process started with. Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("None: no session has been created since the app started"));
    }

    @Test
    public void theSessionCreationTallySitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(countedPaths(SessionCreationPath.RECONNECT_OF_A_DEAD_SESSION));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the tally has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " tally that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }

    @Test
    public void everyCreationPathCarriesWordingOfItsOwn() {
        for (SessionCreationPath path : SessionCreationPath.values()) {
            for (SessionCreationPath otherPath : SessionCreationPath.values()) {
                if (path != otherPath) {
                    Assert.assertNotEquals("two paths sharing wording make the report unable to say"
                            + " which code created a session, which is the only reason this section"
                            + " exists",
                        path.getReportLabel(), otherPath.getReportLabel());
                }
            }
        }
    }
}
