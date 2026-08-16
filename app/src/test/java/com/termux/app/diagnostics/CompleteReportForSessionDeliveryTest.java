package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompleteReportForSessionDeliveryTest {

    private static final int PENDING_MESSAGES_MORE_THAN_THE_PASTE_CEILING_ALLOWS = 30;

    private static final String OMISSION_NOTE = "further lines left out";

    private static DiagnosticsReport reportWithMainLooperQueue(DiagnosticsMainLooperQueue mainLooperQueue) {
        return new DiagnosticsReport("0.119.0", 119, 0L,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0), new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            mainLooperQueue, ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(), DiagnosticsShellExits.NONE,
            DiagnosticsPhantomProcessMonitor.UNMEASURED, DiagnosticsAppProcessPopulation.UNMEASURED,
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0), DiagnosticsSessionCreationPaths.NONE,
            DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE,
            DiagnosticsTouchEvents.NONE, DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
    }

    private static DiagnosticsReport reportLongerThanThePasteCeiling() {
        String[] whenValues = new String[PENDING_MESSAGES_MORE_THAN_THE_PASTE_CEILING_ALLOWS];
        Arrays.fill(whenValues, "+300ms");
        List<String> dumpLines = new ArrayList<>();
        dumpLines.add("Looper (main, tid 2) {abc}");
        for (int messageIndex = 0; messageIndex < whenValues.length; messageIndex++) {
            dumpLines.add("  Message " + messageIndex + ": { when=" + whenValues[messageIndex]
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        dumpLines.add("  (Total messages: " + whenValues.length + ")");
        return reportWithMainLooperQueue(DiagnosticsMainLooperQueue.parse(dumpLines));
    }

    private static int countOfRenderedMessageLines(String renderedReport) {
        int renderedMessageLines = 0;
        for (String line : renderedReport.split("\n")) {
            if (line.trim().startsWith("Message ") && line.contains("{ when=")) renderedMessageLines++;
        }
        return renderedMessageLines;
    }

    @Test
    public void theReportTheOwnerCopiesOutOfTheSettingsScreenStaysWithinThePasteCeiling() {
        String renderedReport = new DiagnosticsReportBuilder().build(reportLongerThanThePasteCeiling());

        Assert.assertTrue("this fixture only proves anything about the delivered report if it is larger"
                + " than what a hand-pasted report may carry, so the settings-screen rendering of it must"
                + " leave lines out. Actual report:\n" + renderedReport,
            renderedReport.contains(OMISSION_NOTE));
        Assert.assertTrue("the ceiling the owner pastes under is what bounds this rendering, so it must"
                + " stay within it. Actual length " + renderedReport.length() + ", report:\n"
                + renderedReport,
            renderedReport.length() <= DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }

    @Test
    public void theReportDeliveredIntoASessionCarriesEveryLineItMeasured() {
        DiagnosticsReport report = reportLongerThanThePasteCeiling();

        String deliveredReport = new DiagnosticsReportBuilder().buildForDeliveryIntoASession(report);

        Assert.assertEquals("the delivered report is written into the session by the app rather than"
                + " copied by hand, so nothing truncates it and every pending message the queue held must"
                + " reach the reader. Actual report:\n" + deliveredReport,
            PENDING_MESSAGES_MORE_THAN_THE_PASTE_CEILING_ALLOWS,
            countOfRenderedMessageLines(deliveredReport));
        Assert.assertFalse("an omission note in a delivered report means the evidence the report exists to"
                + " carry was dropped before anything read it. Actual report:\n" + deliveredReport,
            deliveredReport.contains(OMISSION_NOTE));
    }
}
