package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainLooperQueuePeakInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final long FLOOD_MILLIS = 1783216740000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Highest main looper queue observed since the process started";

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;
        private final boolean mCanHoldScrollbarFadeCallback;
        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        CensusNode(String className, boolean canHoldScrollbarFadeCallback,
                   ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mCanHoldScrollbarFadeCallback = canHoldScrollbarFadeCallback;
            mChildren = new ArrayList<>(Arrays.asList(children));
        }

        @Override
        public boolean canHoldScrollbarFadeCallback() {
            return mCanHoldScrollbarFadeCallback;
        }

        @Override
        public String getClassName() {
            return mClassName;
        }

        @Override
        public List<ScrollbarViewCensus.ViewNode> getChildren() {
            return mChildren;
        }
    }

    private static ScrollbarViewCensus censusOfWebViews(int webViewCount) {
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (int viewIndex = 0; viewIndex < webViewCount; viewIndex++) {
            children.add(new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true));
        }
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            children.toArray(new ScrollbarViewCensus.ViewNode[0])));
    }

    private static DiagnosticsMainLooperQueuePeak peakOfTheShapeTheDeviceReported() {
        List<DiagnosticsMainLooperQueueTarget> busiestTargets = new ArrayList<>();
        busiestTargets.add(new DiagnosticsMainLooperQueueTarget(
            "android.view.ViewRootImpl$ViewRootHandler android.view.View$ScrollabilityCache", 71));
        busiestTargets.add(new DiagnosticsMainLooperQueueTarget(
            "com.termux.terminal.TerminalSession$MainThreadHandler", 11));
        return DiagnosticsMainLooperQueuePeak.observedAt(82, FLOOD_MILLIS, busiestTargets,
            censusOfWebViews(74));
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

    private static String renderedReport(DiagnosticsMainLooperQueuePeak peak,
                                         DiagnosticsMainThreadStalls stalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.3825", 3825, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            stalls,
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()),
            ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST, NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE,
            DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE, peak, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportStatesTheDeepestQueueEverObservedEvenWhenItHasDrained() {
        String report = renderedReport(peakOfTheShapeTheDeviceReported(),
            new DiagnosticsMainThreadStalls(80L, 0L, 0L, "", Collections.emptyList(), 0L, 0L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the queue this report states as it stands holds nothing, so without the highest"
                + " observation a reading taken after the freeze carries no trace of it at all. Actual"
                + " report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the depth is the number the freeze investigation reads. Actual report:\n"
            + report, section.contains("Pending messages: 82 at 2026-07-05T01:59:00Z"));
        Assert.assertTrue("a count with nothing behind it cannot be attributed to what posted the"
                + " messages. Actual report:\n" + report,
            section.contains("71 x android.view.ViewRootImpl$ViewRootHandler"
                + " android.view.View$ScrollabilityCache"));
        Assert.assertTrue("the pending count only means something against how many views could each hold"
            + " one such callback at that same moment. Actual report:\n" + report,
            section.contains("Views that could hold a scrollbar fade callback then: 74"));
        Assert.assertTrue("the view class holding them is where the investigation goes next. Actual"
                + " report:\n" + report,
            section.contains("74 x com.termux.app.browser.BrowserAssistStructureFreeWebView"));
    }

    @Test
    public void theDeepestQueueSurvivesHoweverMuchTheStallEvidenceOccupies() {
        String report = renderedReport(peakOfTheShapeTheDeviceReported(),
            stallsLargeEnoughToFillTheCeilingAlone());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " section that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);

        String window = report.substring(0, Math.min(report.length(),
            DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
        Assert.assertTrue("a heading that survives while the count under it is cut leaves the reader with"
            + " nothing. Actual report:\n" + report,
            window.contains("Pending messages: 82 at 2026-07-05T01:59:00Z"));
        Assert.assertTrue("the busiest target is what attributes the count, so it has to survive beside"
                + " it. Actual report:\n" + report,
            window.contains("71 x android.view.ViewRootImpl$ViewRootHandler"
                + " android.view.View$ScrollabilityCache"));
    }

    @Test
    public void aProcessThatHasNotSampledYetSaysSoRatherThanReadingAsAnEmptyQueue() {
        String report = renderedReport(DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED,
            new DiagnosticsMainThreadStalls(80L, 0L, 0L, "", Collections.emptyList(), 0L, 0L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section is indistinguishable from a section that was never added, and"
                + " the reader would not know the sampling had run. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("a process that has not yet reached its first sampling would otherwise report a"
                + " depth of zero, which reads as a measured quiet queue rather than as no measurement."
                + " Actual report:\n" + report,
            section.contains("Not sampled yet"));
    }
}
