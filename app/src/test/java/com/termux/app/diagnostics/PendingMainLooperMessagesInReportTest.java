package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionReconnectReason;
import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PendingMainLooperMessagesInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final String PENDING_MESSAGE_HEADING = "Pending main looper messages, oldest first";

    private static final String OLDEST_PENDING_MESSAGE_LINE =
        "Message 0: { when=-1s234ms callback=android.view.View$ScrollabilityCache";

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

    private static DiagnosticsMainLooperQueue looperQueueOfTheShapeTheDeviceReported() {
        List<String> dumpLines = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < 71; messageIndex++) {
            dumpLines.add("Message " + messageIndex + ": { when=-1s234ms"
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        return DiagnosticsMainLooperQueue.parse(dumpLines);
    }

    private static ScrollbarViewCensus censusOfTheShapeTheDeviceReported() {
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true),
            new CensusNode("com.termux.view.TerminalView", true)));
    }

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;

        private final boolean mCanHoldScrollbarFadeCallback;

        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        private CensusNode(String className, boolean canHoldScrollbarFadeCallback,
                           ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mCanHoldScrollbarFadeCallback = canHoldScrollbarFadeCallback;
            mChildren = Arrays.asList(children);
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

    private static List<DiagnosticsSessionLine> sessionLines(int sessionCount) {
        List<DiagnosticsSessionLine> sessionLines = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionCount; sessionIndex++) {
            sessionLines.add(new DiagnosticsSessionLine("host-" + sessionIndex, true, sessionIndex, true,
                4000, 108, DiagnosticsSessionListDisplay.DISPLAYED,
                new DiagnosticsShellInputDelivery(0L, 0L, 0L, true, null),
                new DiagnosticsSessionStatusline(null, null, null, SessionNewActivityTier.NONE),
                DiagnosticsScrollGestureRouting.ofEmulatorState(false, false),
                DiagnosticsSessionListAbsence.presentInTheList()));
        }
        return sessionLines;
    }

    private static String reportOfTheShapeTheDeviceProduces() {
        List<DiagnosticsSessionLine> sessionLines = sessionLines(19);
        DiagnosticsReport report = new DiagnosticsReport("0.119.3825", 3825, REPORT_MILLIS,
            sessionLines.size(), sessionLines.size(), 64, sessionLines,
            4, 37, true, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(212, 384, 512, 96),
            new DiagnosticsWorkCostLine(27, 28, 6, 4000),
            new DiagnosticsWorkCostLine(7, 16, 5, 4000),
            new DiagnosticsWorkCostLine(6, 39, 21, 4000),
            new DiagnosticsSessionReconnectCost(3, 812, 604, 2,
                Collections.<DiagnosticsSessionReconnectCostByReason>emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            stallsLargeEnoughToFillTheCeilingAlone(),
            looperQueueOfTheShapeTheDeviceReported(),
            censusOfTheShapeTheDeviceReported(),
            106000L,
            new DiagnosticsBackgroundCycle(60000L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            new DiagnosticsShellExits(Arrays.asList(new DiagnosticsShellExitCount(255, 18))),
            new DiagnosticsPhantomProcessMonitor("true", 32, true),
            DiagnosticsAppProcessPopulation.measured(20,
                Arrays.asList(new DiagnosticsProcessCommandCount("ssh", 19))),
            new DiagnosticsWorkCostLine(184, 96, 12, 4000),
            new DiagnosticsWorkCostLine(902, 141, 9, 4000),
            new DiagnosticsSessionCreationPaths(Arrays.asList(
                new DiagnosticsSessionCreationPathCount(
                    SessionCreationPath.RESTORE_OF_A_PERSISTED_SESSION, 19))),
            new DiagnosticsActivityWindows(7, 3),
            DiagnosticsReportDelivery.of("host-0", 11023, 842L, 4096L, 4097L, true),
            DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED, DiagnosticsScrollSteps.NONE, DiagnosticsTouchEvents.NONE);
        return new DiagnosticsReportBuilder().build(report);
    }

    private static String windowTheReportSurvivesIn(String report) {
        return report.substring(0,
            Math.min(report.length(), DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
    }

    @Test
    public void theAgeOfThePendingMessagesSurvivesHoweverMuchTheStallEvidenceOccupies() {
        String survivingWindow = windowTheReportSurvivesIn(reportOfTheShapeTheDeviceProduces());

        Assert.assertTrue("the heading of the pending message list has to reach the report before its"
                + " lines can. Actual window the report survives in:\n" + survivingWindow,
            survivingWindow.contains(PENDING_MESSAGE_HEADING));
        Assert.assertTrue("each pending message line carries how long that message has been waiting, which"
                + " is what separates a queue the main thread is not draining from a queue that is merely"
                + " long, so stall evidence large enough to fill the ceiling on its own must lose its own"
                + " trailing frames rather than take every line of the list the freeze investigation reads."
                + " Actual window the report survives in:\n" + survivingWindow,
            survivingWindow.contains(OLDEST_PENDING_MESSAGE_LINE));
    }

    @Test
    public void theStallEvidenceStillReachesTheReportBesideThePendingMessages() {
        String survivingWindow = windowTheReportSurvivesIn(reportOfTheShapeTheDeviceProduces());

        Assert.assertTrue("giving the pending message list its budget must not remove the stall evidence,"
                + " which attributes the stalls themselves. Actual window the report survives in:\n"
                + survivingWindow,
            survivingWindow.contains("Stalls over 80 ms"));
        Assert.assertTrue("the longest stall is the line the stall evidence exists to carry. Actual window"
                + " the report survives in:\n" + survivingWindow,
            survivingWindow.contains("Longest: 9876 ms"));
    }
}
