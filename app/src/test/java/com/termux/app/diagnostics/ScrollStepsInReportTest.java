package com.termux.app.diagnostics;

import com.termux.view.scroll.TerminalScrollEvent;
import com.termux.view.scroll.TerminalScrollStepCounter;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ScrollStepsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Scroll steps since the app started";

    private static String renderedReportOf(DiagnosticsScrollSteps scrollSteps) {
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
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, scrollSteps, DiagnosticsTouchEvents.NONE,
            DiagnosticsPreviousProcessExits.NOT_TAKEN);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsScrollSteps stepsTakenAt(TerminalScrollEvent destination,
                                                       long... steppedAtMillis) {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();
        for (long stepMillis : steppedAtMillis) {
            counter.record(destination, stepMillis);
        }
        return DiagnosticsScrollSteps.of(counter);
    }

    @Test
    public void aGestureSentToTheShellIsCountedSoItCanBeToldFromNoGestureAtAll() {
        String report = renderedReportOf(stepsTakenAt(TerminalScrollEvent.MOUSE_WHEEL,
            REPORT_MILLIS - 4000L, REPORT_MILLIS - 2000L, REPORT_MILLIS - 1000L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("without this section a reading taken while scrolling is dead cannot say"
                + " whether the gesture was delivered to the shell and ignored or never reached the"
                + " scrolling code, and those two have different causes. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the count is what shows the gesture was made at all. Actual report:\n" + report,
            section.contains("  To the shell as a mouse wheel: 3, most recent "));
        Assert.assertTrue("the time is what places the gesture inside the window the owner reported."
            + " Actual report:\n" + report, section.contains("2026-07-05T01:59:59Z\n"));
        Assert.assertTrue("a total is what a reader checks first to rule out that no gesture happened."
            + " Actual report:\n" + report, section.contains("  Total: 3\n"));
    }

    @Test
    public void aDestinationNoStepWentToIsLeftOutRatherThanPrintedAsZero() {
        String report = renderedReportOf(stepsTakenAt(TerminalScrollEvent.MOUSE_WHEEL, REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertFalse("the report is cut at a paste limit and a line spent on a destination nothing"
                + " went to is a line taken from evidence that is already being truncated."
                + " Actual report:\n" + report,
            report.substring(sectionIndex).contains("To the view's own scrollback"));
    }

    @Test
    public void aRunInWhichNothingWasScrolledIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsScrollSteps.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is the state a"
                + " reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("no gesture having reached the scrolling code is itself the finding that"
                + " separates a swallowed gesture from one the touch handling never delivered."
                + " Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("None: no scroll gesture has reached the terminal view yet"));
    }

    @Test
    public void theScrollTallySitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(stepsTakenAt(TerminalScrollEvent.ARROW_KEY, REPORT_MILLIS));

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
    public void everyDestinationCarriesWordingOfItsOwn() {
        for (TerminalScrollEvent destination : TerminalScrollEvent.values()) {
            for (TerminalScrollEvent otherDestination : TerminalScrollEvent.values()) {
                if (destination != otherDestination) {
                    Assert.assertNotEquals("two destinations sharing wording make the report unable to"
                            + " say where a gesture went, which is the only reason this section exists",
                        DiagnosticsScrollDestinationLabel.of(destination),
                        DiagnosticsScrollDestinationLabel.of(otherDestination));
                }
            }
        }
    }
}
