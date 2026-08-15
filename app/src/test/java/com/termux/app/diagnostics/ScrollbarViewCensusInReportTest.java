package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScrollbarViewCensusInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Views that can hold a scrollbar fade callback";

    private static final class FakeViewNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;
        private final boolean mCanHoldScrollbarFadeCallback;
        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        FakeViewNode(String className, boolean canHoldScrollbarFadeCallback,
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

    private static String renderedReportOf(ScrollbarViewCensus scrollbarViewCensus) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), scrollbarViewCensus, 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE, DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportNamesTheViewClassesThatCanHoldAScrollbarFadeCallbackAndHowManyOfEach() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.<ScrollbarViewCensus.ViewNode>singletonList(
                new FakeViewNode("android.widget.FrameLayout", false,
                    new FakeViewNode("android.webkit.WebView", true),
                    new FakeViewNode("android.webkit.WebView", true),
                    new FakeViewNode("com.termux.view.TerminalView", true))), 0));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the pending scrollbar fade callbacks in the main looper queue carry only the"
                + " callback class name, so without this section the views holding them cannot be named"
                + " from a measurement at all. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the total is what the pending callback count is compared against. Actual"
            + " report:\n" + report, section.contains("Total: 3"));
        Assert.assertTrue("the class holding the most such views is the one the investigation goes to"
            + " first. Actual report:\n" + report, section.contains("2 x android.webkit.WebView"));
        Assert.assertTrue("a class holding only one still has to appear, because a single unexpected view"
                + " is as much of a finding as a crowd of them. Actual report:\n" + report,
            section.contains("1 x com.termux.view.TerminalView"));
    }

    @Test
    public void aWindowHoldingNoSuchViewSaysSoRatherThanOmittingTheSection() {
        String report = renderedReportOf(ScrollbarViewCensus.empty());

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section is indistinguishable from a section that was never added,"
                + " and the reader would not know the walk had run. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("a measured zero is a finding, because it says the views holding the pending"
            + " callbacks are not in this window. Actual report:\n" + report, section.contains("Total: 0"));
        Assert.assertTrue("an empty class list has to be stated rather than left blank. Actual report:\n"
            + report, section.contains("Busiest classes: none"));
    }

    @Test
    public void theCensusSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.<ScrollbarViewCensus.ViewNode>singletonList(
                new FakeViewNode("com.termux.view.TerminalView", true)), 0));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " section that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
