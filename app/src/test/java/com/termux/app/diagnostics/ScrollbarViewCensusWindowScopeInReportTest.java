package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScrollbarViewCensusWindowScopeInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Views that can hold a scrollbar fade callback";

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;

        private final boolean mCanHoldScrollbarFadeCallback;

        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        private CensusNode(String className, boolean canHoldScrollbarFadeCallback,
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
        @NonNull
        public String getClassName() {
            return mClassName;
        }

        @Override
        @NonNull
        public List<ScrollbarViewCensus.ViewNode> getChildren() {
            return mChildren;
        }
    }

    private static ScrollbarViewCensus.ViewNode windowHoldingOneTerminalView() {
        return new CensusNode("android.widget.FrameLayout", false,
            new CensusNode("com.termux.view.TerminalView", true));
    }

    private static String renderedReportOf(ScrollbarViewCensus census) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), census, 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED,
            DiagnosticsAppProcessPopulation.UNMEASURED,
            NO_WORK_COST,
            NO_WORK_COST, DiagnosticsSessionCreationPaths.NONE, DiagnosticsActivityWindows.NONE,
            DiagnosticsReportDelivery.NONE,
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN,
            ProcessConditionSnapshot.NOT_RECORDED);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static String scrollbarSectionOf(String report) {
        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the scrollbar view count is the reading the pending fade callbacks are compared"
            + " against, so its absence leaves those callbacks unattributable. Actual report:\n" + report,
            sectionIndex >= 0);
        return report.substring(sectionIndex);
    }

    @Test
    public void theNumberOfWindowsBehindTheCountIsStatedBesideIt() {
        String report = renderedReportOf(ScrollbarViewCensus.take(Arrays.asList(
            windowHoldingOneTerminalView(), windowHoldingOneTerminalView(),
            windowHoldingOneTerminalView()), 0));

        Assert.assertTrue("a reader comparing this total against the pending fade callbacks cannot tell a"
                + " count taken over the whole process from one taken over a single window unless the"
                + " number of windows behind it is stated. Actual report:\n" + report,
            scrollbarSectionOf(report).contains("Windows walked: 3"));
    }

    @Test
    public void theWindowsWhoseViewsCanNoLongerBeReachedAreStated() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.singletonList(windowHoldingOneTerminalView()), 2));

        Assert.assertTrue("the views of a released window can still hold a queued fade callback, so a total"
                + " that silently leaves those windows out reads as a complete count when it is not."
                + " Actual report:\n" + report,
            scrollbarSectionOf(report).contains("Windows no longer reachable: 2"));
    }

    @Test
    public void aProcessHoldingEveryWindowItBuiltSaysNoneWereReleasedRatherThanOmittingTheLine() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.singletonList(windowHoldingOneTerminalView()), 0));

        Assert.assertTrue("an absent line reads as an unmeasured quantity, and that every window is still"
                + " reachable is itself the finding that makes the total trustworthy. Actual report:\n" + report,
            scrollbarSectionOf(report).contains("Windows no longer reachable: 0"));
    }

    @Test
    public void theWindowScopeLinesAreStatedEvenWhenNoViewCanHoldAFadeCallback() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.<ScrollbarViewCensus.ViewNode>singletonList(
                new CensusNode("android.widget.FrameLayout", false)), 1));

        String section = scrollbarSectionOf(report);
        Assert.assertTrue("a total of zero taken over one reachable window out of two is a different"
                + " finding from a total of zero taken over every window, and the reader has to be able to"
                + " tell them apart. Actual report:\n" + report,
            section.contains("Windows walked: 1"));
        Assert.assertTrue("Actual report:\n" + report,
            section.contains("Windows no longer reachable: 1"));
    }

    @Test
    public void aCensusTakenBeforeAnyWindowWasRegisteredSpendsNoCharactersOnWindowScope() {
        String report = renderedReportOf(ScrollbarViewCensus.empty());

        Assert.assertFalse("the report reaches the reader only as its first "
                + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, and a census with no"
                + " window to describe would be restating the zero already on the total line while pushing"
                + " measured evidence out of that window. Actual report:\n" + report,
            report.contains("Windows walked"));
    }

    @Test
    public void theWindowScopeLinesSitInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(ScrollbarViewCensus.take(
            Collections.singletonList(windowHoldingOneTerminalView()), 0));

        int walkedIndex = report.indexOf("Windows walked: 1");
        Assert.assertTrue("the line has to be present for its position to matter. Actual report:\n" + report,
            walkedIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only the"
                + " first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a line that"
                + " falls outside that window cannot be read at all. It currently begins at " + walkedIndex
                + ". Actual report:\n" + report,
            walkedIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
