package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionReconnectReason;
import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReportCharacterCeilingAllocatedByPriorityTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final String FIRST_PENDING_MESSAGE_LINE_MARKER = "Message 0: { when=";

    private static final String OMISSION_NOTE_MARKER = "further lines left out";

    private static String stackTraceOf(String methodName, int frameCount) {
        StringBuilder stackTrace = new StringBuilder();
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            if (frameIndex > 0) stackTrace.append('\n');
            stackTrace.append("at ").append(methodName).append(frameIndex)
                .append("(MaterialShapeDrawable.java:").append(1000 + frameIndex).append(')');
        }
        return stackTrace.toString();
    }

    private static DiagnosticsMainThreadStalls stallsLargeEnoughToFillTheCeilingAlone() {
        List<MainThreadStallHotPath> hotPaths = new ArrayList<>();
        for (int hotPathIndex = 0; hotPathIndex < 24; hotPathIndex++) {
            hotPaths.add(new MainThreadStallHotPath(
                stackTraceOf("com.google.android.material.shape.ShapePath.path" + hotPathIndex, 64),
                37L, 98765L, 4321L));
        }
        return new DiagnosticsMainThreadStalls(80L, 4321L, 9876L,
            stackTraceOf("com.google.android.material.shape.ShapePath.lineTo", 64), hotPaths, 4321L, 0L);
    }

    private static DiagnosticsMainThreadStalls stallsThatFitEasily() {
        return new DiagnosticsMainThreadStalls(80L, 1L, 121L,
            stackTraceOf("com.termux.view.TerminalView.onDraw", 2),
            Collections.<MainThreadStallHotPath>emptyList(), 1L, 0L);
    }

    private static DiagnosticsMainLooperQueue looperQueueOfTheShapeTheDeviceReported() {
        List<String> dumpLines = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < 71; messageIndex++) {
            dumpLines.add("Message " + messageIndex + ": { when=-1s234ms"
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        return DiagnosticsMainLooperQueue.parse(dumpLines);
    }

    private static DiagnosticsMainLooperQueue looperQueueOfOneMessage() {
        return DiagnosticsMainLooperQueue.parse(Collections.singletonList(
            "Message 0: { when=-1ms what=0 target=com.termux.terminal.TerminalSession$MainThreadHandler }"));
    }

    private static ScrollbarViewCensus censusOfOneWindow() {
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            new CensusNode("com.termux.view.TerminalView", true),
            new CensusNode("androidx.recyclerview.widget.RecyclerView", true)));
    }

    private static ScrollbarViewCensus censusOfMoreClassesThanFit() {
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (int classIndex = 0; classIndex < 8; classIndex++) {
            children.add(new CensusNode(
                "com.termux.app.browser.BrowserAssistStructureFreeWebViewOfKind" + classIndex, true));
        }
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            children.toArray(new ScrollbarViewCensus.ViewNode[0])));
    }

    private static DiagnosticsMainLooperQueuePeak peakLargeEnoughToCrowdOutWhatFollowsIt() {
        List<DiagnosticsMainLooperQueueTarget> busiestTargets = new ArrayList<>();
        for (int targetIndex = 0; targetIndex < 5; targetIndex++) {
            busiestTargets.add(new DiagnosticsMainLooperQueueTarget(
                "android.view.ViewRootImpl$ViewRootHandler"
                    + " android.view.View$ScrollabilityCacheOfKind" + targetIndex, 137 - targetIndex));
        }
        return DiagnosticsMainLooperQueuePeak.observedAt(683, REPORT_MILLIS - 60000L, busiestTargets,
            censusOfMoreClassesThanFit());
    }

    private static List<DiagnosticsSessionLine> sessionLines(int sessionCount, long acceptedButNotWritten) {
        List<DiagnosticsSessionLine> sessionLines = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionCount; sessionIndex++) {
            sessionLines.add(new DiagnosticsSessionLine("host-" + sessionIndex, true, sessionIndex, true,
                4000, 108, DiagnosticsSessionListDisplay.DISPLAYED,
                new DiagnosticsShellInputDelivery(acceptedButNotWritten, 0L, 0L, true, null),
                new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.NONE),
                DiagnosticsScrollGestureRouting.ofEmulatorState(false, false),
                DiagnosticsSessionListAbsence.presentInTheList()));
        }
        return sessionLines;
    }

    private static DiagnosticsShellExits shellExitsOfEveryStatusTheShellCanReport() {
        List<DiagnosticsShellExitCount> exitCounts = new ArrayList<>();
        for (int exitStatus = 0; exitStatus < 64; exitStatus++) {
            exitCounts.add(new DiagnosticsShellExitCount(exitStatus, 64 - exitStatus));
        }
        return new DiagnosticsShellExits(exitCounts);
    }

    private static DiagnosticsSessionCreationPaths creationPathsOfEveryPathTheAppHas() {
        List<DiagnosticsSessionCreationPathCount> pathCounts = new ArrayList<>();
        for (SessionCreationPath path : SessionCreationPath.values()) {
            pathCounts.add(new DiagnosticsSessionCreationPathCount(path, 19));
        }
        return new DiagnosticsSessionCreationPaths(pathCounts);
    }

    private static DiagnosticsAppProcessPopulation processPopulationOfMoreCommandsThanFit() {
        List<DiagnosticsProcessCommandCount> commandCounts = new ArrayList<>();
        for (int commandIndex = 0; commandIndex < 40; commandIndex++) {
            commandCounts.add(new DiagnosticsProcessCommandCount(
                "com.termux.process.with.a.long.command.name." + commandIndex, 40 - commandIndex));
        }
        return DiagnosticsAppProcessPopulation.measured(820, commandCounts);
    }

    private static DiagnosticsSessionReconnectCost reconnectCostOfEveryReasonTheAppHas() {
        List<DiagnosticsSessionReconnectCostByReason> costsByReason = new ArrayList<>();
        for (SessionReconnectReason reason : SessionReconnectReason.values()) {
            costsByReason.add(new DiagnosticsSessionReconnectCostByReason(reason, 19, 8123, 4071));
        }
        return new DiagnosticsSessionReconnectCost(76, 32492, 4071, 19, costsByReason);
    }

    private static String renderedReport(DiagnosticsMainThreadStalls stalls,
                                         DiagnosticsMainLooperQueue looperQueue,
                                         DiagnosticsMainLooperQueuePeak peak,
                                         ScrollbarViewCensus census,
                                         List<DiagnosticsSessionLine> sessionLines,
                                         DiagnosticsShellExits shellExits,
                                         DiagnosticsSessionCreationPaths creationPaths,
                                         DiagnosticsAppProcessPopulation processPopulation,
                                         DiagnosticsSessionReconnectCost reconnectCost,
                                         DiagnosticsReplacedSessionShellInput replacedSessionInput) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.3831", 3831, REPORT_MILLIS,
            sessionLines.size(), sessionLines.size(), 64, sessionLines,
            4, 37, true, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(212, 384, 512, 96),
            new DiagnosticsWorkCostLine(27, 28, 6, 4000),
            new DiagnosticsWorkCostLine(7, 16, 5, 4000),
            new DiagnosticsWorkCostLine(6, 39, 21, 4000),
            reconnectCost,
            replacedSessionInput,
            stalls,
            looperQueue,
            census,
            106000L,
            new DiagnosticsBackgroundCycle(60000L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            shellExits,
            new DiagnosticsPhantomProcessMonitor("true", 32, true),
            processPopulation,
            new DiagnosticsWorkCostLine(184, 96, 12, 4000),
            new DiagnosticsWorkCostLine(902, 141, 9, 4000),
            creationPaths,
            new DiagnosticsActivityWindows(7, 3),
            DiagnosticsReportDelivery.of("host-0", 11023, 842L, 4096L, 4097L, true),
            peak, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static String reportWithEverySubsectionAheadOfThePendingMessagesAtItsLargest() {
        return renderedReport(stallsLargeEnoughToFillTheCeilingAlone(),
            looperQueueOfTheShapeTheDeviceReported(),
            peakLargeEnoughToCrowdOutWhatFollowsIt(),
            censusOfMoreClassesThanFit(),
            sessionLines(19, 4096L),
            shellExitsOfEveryStatusTheShellCanReport(), creationPathsOfEveryPathTheAppHas(),
            processPopulationOfMoreCommandsThanFit(), reconnectCostOfEveryReasonTheAppHas(),
            new DiagnosticsReplacedSessionShellInput(19, 17, 40960L,
                "host-with-a-long-session-name-0", "host-with-a-long-session-name-1",
                "the shell process the writer was writing to had already gone"));
    }

    private static String reportSmallEnoughToLeaveTheCeilingUnused() {
        return renderedReport(stallsThatFitEasily(),
            looperQueueOfOneMessage(),
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED,
            censusOfOneWindow(),
            sessionLines(1, 0L),
            new DiagnosticsShellExits(Collections.singletonList(new DiagnosticsShellExitCount(0, 1))),
            new DiagnosticsSessionCreationPaths(Collections.singletonList(
                new DiagnosticsSessionCreationPathCount(SessionCreationPath.NEW_AUTOSSH_SESSION, 1))),
            DiagnosticsAppProcessPopulation.measured(3, Collections.singletonList(
                new DiagnosticsProcessCommandCount("ssh", 3))),
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0,
                Collections.<DiagnosticsSessionReconnectCostByReason>emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""));
    }

    private static String windowTheReportSurvivesIn(String report) {
        return report.substring(0,
            Math.min(report.length(), DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
    }

    @Test
    public void thePendingMessageAgesSurviveEverySubsectionPrintedAheadOfThemAtItsLargest() {
        String report = reportWithEverySubsectionAheadOfThePendingMessagesAtItsLargest();

        Assert.assertTrue("the report has to be longer than the window it survives in, otherwise nothing"
                + " is dropped and this test proves nothing, but it is only " + report.length()
                + " characters. Actual report:\n" + report,
            report.length() > DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);

        String survivingWindow = windowTheReportSurvivesIn(report);
        Assert.assertTrue("the age each pending main looper message carries is what separates a queue the"
                + " main thread has stopped draining from one that is merely long, and it must not be lost"
                + " to subsections that are printed earlier but matter less, whatever those subsections"
                + " render. Actual window the report survives in:\n" + survivingWindow,
            survivingWindow.contains(FIRST_PENDING_MESSAGE_LINE_MARKER));
    }

    @Test
    public void aReportWithRoomToSpareOmitsNoLineFromAnySubsection() {
        String report = reportSmallEnoughToLeaveTheCeilingUnused();

        Assert.assertTrue("this report has to fit inside the window so that any omission in it comes from"
                + " reserving characters that nothing needed, but it is " + report.length()
                + " characters. Actual report:\n" + report,
            report.length() < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);

        Assert.assertFalse("reserving characters for a later subsection must not cost an earlier one lines"
                + " it had room to print, so a report that fits inside the window whole must drop nothing"
                + " anywhere. Actual report:\n" + report,
            report.contains(OMISSION_NOTE_MARKER));
    }

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;

        private final boolean mCanHoldScrollbarFadeCallback;

        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        CensusNode(String className, boolean canHoldScrollbarFadeCallback,
                   ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mCanHoldScrollbarFadeCallback = canHoldScrollbarFadeCallback;
            mChildren = Arrays.asList(children);
        }

        @Override
        public String getClassName() {
            return mClassName;
        }

        @Override
        public boolean canHoldScrollbarFadeCallback() {
            return mCanHoldScrollbarFadeCallback;
        }

        @Override
        public List<ScrollbarViewCensus.ViewNode> getChildren() {
            return mChildren;
        }
    }
}
