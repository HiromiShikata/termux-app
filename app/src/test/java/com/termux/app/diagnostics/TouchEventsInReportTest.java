package com.termux.app.diagnostics;

import com.termux.view.touch.TerminalTouchCounter;
import com.termux.view.touch.TerminalTouchKind;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TouchEventsInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Touch events the terminal view received since the app started";

    private static String renderedReportOf(DiagnosticsTouchEvents touchEvents) {
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
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, touchEvents,
            DiagnosticsPreviousProcessExits.NOT_TAKEN);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static DiagnosticsTouchEvents touchesReceivedAt(TerminalTouchKind kind,
                                                            long... receivedAtMillis) {
        TerminalTouchCounter counter = new TerminalTouchCounter();
        for (long receivedMillis : receivedAtMillis) {
            counter.record(kind, receivedMillis);
        }
        return DiagnosticsTouchEvents.of(counter);
    }

    @Test
    public void aTouchThatReachedTheViewIsCountedSoItCanBeToldFromOneStoppedBeforeIt() {
        String report = renderedReportOf(touchesReceivedAt(TerminalTouchKind.GESTURE_MOVEMENT,
            REPORT_MILLIS - 4000L, REPORT_MILLIS - 2000L, REPORT_MILLIS - 1000L));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("without this section a reading in which no scroll step was taken cannot say"
                + " whether the touch was consumed before the terminal view or reached it and was not"
                + " recognised as a scroll, and those two have different causes. Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("the count is what shows the view was touched at all. Actual report:\n" + report,
            section.contains("  Of movement within a gesture: 3, most recent "));
        Assert.assertTrue("the time is what places the touch inside the window the owner reported."
            + " Actual report:\n" + report, section.contains("2026-07-05T01:59:59Z\n"));
        Assert.assertTrue("a total is what a reader checks first to rule out that nothing was touched."
            + " Actual report:\n" + report, section.contains("  Total: 3\n"));
    }

    @Test
    public void aKindNoTouchArrivedAsIsLeftOutRatherThanPrintedAsZero() {
        String report = renderedReportOf(touchesReceivedAt(TerminalTouchKind.GESTURE_START, REPORT_MILLIS));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to exist before its contents can be judged. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertFalse("the report is cut at a paste limit and a line spent on a kind nothing arrived"
                + " as is a line taken from evidence that is already being truncated."
                + " Actual report:\n" + report,
            report.substring(sectionIndex).contains("Of the end of a gesture"));
    }

    @Test
    public void aRunInWhichTheViewWasNeverTouchedIsReportedAsMeasuredRatherThanOmitted() {
        String report = renderedReportOf(DiagnosticsTouchEvents.NONE);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as an unmeasured application, which is the state a"
                + " reader must be able to rule out. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("the terminal view never having been touched is itself the finding that points"
                + " at something above it consuming the gesture. Actual report:\n" + report,
            report.substring(sectionIndex)
                .contains("None: the terminal view has received no touch at all"));
    }

    @Test
    public void theTouchTallySitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(touchesReceivedAt(TerminalTouchKind.GESTURE_START, REPORT_MILLIS));

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
    public void everyTouchKindCarriesWordingOfItsOwn() {
        for (TerminalTouchKind kind : TerminalTouchKind.values()) {
            for (TerminalTouchKind otherKind : TerminalTouchKind.values()) {
                if (kind != otherKind) {
                    Assert.assertNotEquals("two kinds sharing wording make the report unable to say what"
                            + " the view received, which is the only reason this section exists",
                        DiagnosticsTouchKindLabel.of(kind),
                        DiagnosticsTouchKindLabel.of(otherKind));
                }
            }
        }
    }
}
