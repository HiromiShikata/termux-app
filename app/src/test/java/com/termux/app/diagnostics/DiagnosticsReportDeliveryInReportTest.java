package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class DiagnosticsReportDeliveryInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Last diagnostics report delivery";

    private static String renderedReportOf(DiagnosticsReportDelivery delivery) {
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
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE, delivery,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void anEnterThatWasNotAcceptedIsNamedSoTheUnsubmittedReportCanBeAttributed() {
        String report = renderedReportOf(
            DiagnosticsReportDelivery.of("session-one", 11023, 842L, 4096L, 4096L, false));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("a report pasted into a session and left unsubmitted cannot be attributed to the"
                + " carriage return being discarded rather than being ignored by the program reading the"
                + " terminal unless the application says which happened. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("Actual report:\n" + report,
            section.contains("Enter accepted for delivery: no"));
        Assert.assertTrue("whether input still reached the program separates a session that stopped taking"
                + " input during the paste from a closed queue. Actual report:\n" + report,
            section.contains("Input reached the program after the paste: no"));
    }

    @Test
    public void aDeliveryThatCompletedIsReportedWithWhatItPastedAndHowLongItTook() {
        String report = renderedReportOf(
            DiagnosticsReportDelivery.of("session-one", 11023, 842L, 4096L, 4097L, true));

        String section = report.substring(report.indexOf(SECTION_HEADING));
        Assert.assertTrue("the session has to be named, because a delivery to one session cannot explain a"
            + " missing report in another. Actual report:\n" + report, section.contains("Session: session-one"));
        Assert.assertTrue("the size and the time the paste occupied the caller are what show whether the"
                + " delivery spanned long enough for the session state to change under it. Actual report:\n"
                + report,
            section.contains("Pasted: 11023 characters in 842 ms"));
        Assert.assertTrue("Actual report:\n" + report,
            section.contains("Enter accepted for delivery: yes"));
    }

    @Test
    public void aProcessThatDeliveredNoReportSaysSoRatherThanBeingLeftBlank() {
        String report = renderedReportOf(DiagnosticsReportDelivery.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an application that was never asked, which is exactly"
                + " the state a reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("None: no report has been delivered to a session yet"));
    }

    @Test
    public void theDeliveryLineSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(
            DiagnosticsReportDelivery.of("session-one", 11023, 842L, 4096L, 4096L, false));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only the"
                + " first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, and a delivery"
                + " that failed is exactly the case where the reader needs this line. It currently begins at "
                + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
