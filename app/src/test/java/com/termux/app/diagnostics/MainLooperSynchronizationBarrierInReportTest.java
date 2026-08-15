package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainLooperSynchronizationBarrierInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final int MESSAGES_BEHIND_THE_BARRIER = 66;

    private static List<String> theDumpTheDeviceReportedWhileTheInterfaceWasFrozen() {
        List<String> dumpLines = new ArrayList<>();
        dumpLines.add("Looper (main, tid 2) {b1a2c3}");
        dumpLines.add("  Message 0: { when=-11ms barrier=2372 }");
        dumpLines.add("  Message 1: { when=-4ms"
            + " callback=com.termux.app.TermuxService$ExternalSyntheticLambda7"
            + " target=android.os.Handler }");
        for (int messageIndex = 2; messageIndex <= MESSAGES_BEHIND_THE_BARRIER; messageIndex++) {
            dumpLines.add("  Message " + messageIndex + ": { when=+425ms"
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        return dumpLines;
    }

    private static DiagnosticsMainThreadStalls stallsLargeEnoughToFillTheCeilingAlone() {
        List<MainThreadStallHotPath> hotPaths = new ArrayList<>();
        for (int hotPathIndex = 0; hotPathIndex < 6; hotPathIndex++) {
            hotPaths.add(new MainThreadStallHotPath(stackTraceOf("hotPath" + hotPathIndex, 60),
                1L, 1571L - hotPathIndex, 1571L - hotPathIndex));
        }
        return new DiagnosticsMainThreadStalls(80L, 32L, 1571L, stackTraceOf("longestStall", 60),
            hotPaths, 32L, 0L);
    }

    private static String stackTraceOf(String methodName, int frameCount) {
        StringBuilder stackTrace = new StringBuilder();
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            if (frameIndex > 0) stackTrace.append('\n');
            stackTrace.append("at com.google.android.material.shape.ShapePath.").append(methodName)
                .append(frameIndex).append("(MaterialShapeDrawable.java:").append(1000 + frameIndex)
                .append(')');
        }
        return stackTrace.toString();
    }

    private static String renderedReport(DiagnosticsMainLooperQueue looperQueue,
                                         DiagnosticsMainThreadStalls stalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.3877", 3877, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            stalls,
            looperQueue,
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
    public void aHeldLooperIsStatedInWordsRatherThanLeftInsideTheRawDump() {
        String report = renderedReport(
            DiagnosticsMainLooperQueue.parse(theDumpTheDeviceReportedWhileTheInterfaceWasFrozen()),
            new DiagnosticsMainThreadStalls(80L, 0L, 0L, "", Collections.emptyList(), 0L, 0L));

        Assert.assertTrue("a process whose looper is held by a barrier runs no synchronous main-thread"
                + " work at all, which is the whole of the reported symptom, and the reading has to say"
                + " so without the reader decoding a platform dump line. Actual report:\n" + report,
            report.contains("Synchronization barriers in the queue: 1"));
        Assert.assertTrue("without the due time and the held count the barrier cannot be told apart from"
                + " one that is about to be lifted in the ordinary course of a frame. Actual report:\n"
                + report,
            report.contains("Oldest barrier due -11ms, with " + MESSAGES_BEHIND_THE_BARRIER
                + " messages queued behind it"));
    }

    @Test
    public void theBarrierLinesSurviveHoweverMuchTheStallEvidenceOccupies() {
        String report = renderedReport(
            DiagnosticsMainLooperQueue.parse(theDumpTheDeviceReportedWhileTheInterfaceWasFrozen()),
            stallsLargeEnoughToFillTheCeilingAlone());

        String window = report.substring(0, Math.min(report.length(),
            DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
        Assert.assertTrue("the reading reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " barrier stated outside that window is a barrier nobody reads. Actual report:\n"
                + report,
            window.contains("Synchronization barriers in the queue: 1"));
        Assert.assertTrue("the count alone does not say how long the interface has been held. Actual"
                + " report:\n" + report,
            window.contains("Oldest barrier due -11ms, with " + MESSAGES_BEHIND_THE_BARRIER
                + " messages queued behind it"));
    }

    @Test
    public void aLooperNothingIsHoldingStatesZeroRatherThanOmittingTheLine() {
        String report = renderedReport(
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            new DiagnosticsMainThreadStalls(80L, 0L, 0L, "", Collections.emptyList(), 0L, 0L));

        Assert.assertTrue("an omitted line is indistinguishable from a build that never measured"
                + " barriers, so a healthy looper would leave the freeze investigation no further"
                + " forward. Actual report:\n" + report,
            report.contains("Synchronization barriers in the queue: 0"));
        Assert.assertFalse("there is no oldest barrier to describe when none is present. Actual"
                + " report:\n" + report,
            report.contains("Oldest barrier due"));
    }
}
