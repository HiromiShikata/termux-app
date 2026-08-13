package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A report taken while the app was slow showed 84 pending main-looper messages, 77 of them the
 * scrollbar fade callback. The count alone cannot tell a burst the app is causing from a queue that
 * is simply not draining. The due time that every message already carries in the looper dump can,
 * and the report was discarding it.
 */
public class PendingMainLooperMessageLinesTest {

    private static final int PENDING_MESSAGES_MORE_THAN_ONE_REPORT_CAN_RENDER = 50;

    private static DiagnosticsReport reportWithMainLooperQueue(DiagnosticsMainLooperQueue mainLooperQueue) {
        return new DiagnosticsReport("0.119.0", 119, 0L,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0), new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", java.util.Collections.emptyList(), 0L, 0L), mainLooperQueue, ScrollbarViewCensus.empty(), 0L, new DiagnosticsBackgroundCycle(0L, java.util.Collections.<BackgroundCycleInterval>emptyList()), DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(), DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED, DiagnosticsAppProcessPopulation.UNMEASURED,
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0), DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE);
    }

    private static List<String> looperDumpWithMessages(String... whenValues) {
        List<String> dumpLines = new ArrayList<>();
        dumpLines.add("Looper (main, tid 2) {abc}");
        for (int messageIndex = 0; messageIndex < whenValues.length; messageIndex++) {
            dumpLines.add("  Message " + messageIndex + ": { when=" + whenValues[messageIndex]
                + " callback=android.view.View$ScrollabilityCache target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        dumpLines.add("  (Total messages: " + whenValues.length + ")");
        return dumpLines;
    }

    private static String renderReportFor(String... whenValues) {
        return new DiagnosticsReportBuilder()
            .build(reportWithMainLooperQueue(DiagnosticsMainLooperQueue.parse(looperDumpWithMessages(whenValues))));
    }

    private static int countOfRenderedMessageLines(String renderedReport) {
        int renderedMessageLines = 0;
        for (String line : renderedReport.split("\n")) {
            if (line.trim().startsWith("Message ") && line.contains("{ when=")) renderedMessageLines++;
        }
        return renderedMessageLines;
    }

    @Test
    public void theDueTimeOfEveryPendingMessageReachesTheReport() {
        String renderedReport = renderReportFor("-1s200ms", "+300ms");

        Assert.assertEquals("the due time of each pending message is the only thing that separates a burst"
                + " the app is causing from a queue that is not draining, so every pending message must be"
                + " rendered. Actual report:\n" + renderedReport,
            2, countOfRenderedMessageLines(renderedReport));
        Assert.assertTrue("a message already overdue by 1s200ms is evidence the queue is not draining, so its"
                + " due time must survive into the report. Actual report:\n" + renderedReport,
            renderedReport.contains("when=-1s200ms"));
        Assert.assertTrue("the callback must survive into the report so the message can be attributed to the"
                + " code that posted it. Actual report:\n" + renderedReport,
            renderedReport.contains("android.view.View$ScrollabilityCache"));
    }

    @Test
    public void theRenderedLinesAreBoundedSoOneReportStaysReadable() {
        String[] whenValues = new String[PENDING_MESSAGES_MORE_THAN_ONE_REPORT_CAN_RENDER];
        Arrays.fill(whenValues, "+300ms");

        String renderedReport = renderReportFor(whenValues);

        int renderedMessageLineCount = countOfRenderedMessageLines(renderedReport);
        Assert.assertTrue("the report is read after being pasted into a channel that keeps only the first "
                + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a queue this long must"
                + " render fewer lines than it holds. Actual report:\n" + renderedReport,
            renderedMessageLineCount < whenValues.length);
        Assert.assertTrue("a queue this busy is the evidence the report exists to carry, so the rendering must"
                + " not collapse to nothing. Actual report:\n" + renderedReport,
            renderedMessageLineCount > 0);
        Assert.assertTrue("capping the rendered lines must not change the reported total. Actual report:\n"
                + renderedReport,
            renderedReport.contains("Pending messages: " + whenValues.length));
        Assert.assertTrue("a shortened list that does not say it was shortened reads as the whole queue, so the"
                + " count left out must be stated. Actual report:\n" + renderedReport,
            renderedReport.contains("further lines left out"));
    }

    @Test
    public void aQueueWithNoPendingMessageRendersNoMessageLine() {
        String renderedReport = renderReportFor();

        Assert.assertEquals("an empty queue has no due time to report, so it must add no line. Actual report:\n"
                + renderedReport,
            0, countOfRenderedMessageLines(renderedReport));
    }
}
