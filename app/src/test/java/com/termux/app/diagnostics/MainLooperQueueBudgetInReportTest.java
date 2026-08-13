package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionReconnectReason;
import com.termux.app.terminal.SessionNewActivityTier;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainLooperQueueBudgetInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final int SCROLLBAR_FADE_MESSAGE_COUNT = 71;

    private static final String SCROLLBAR_FADE_TARGET_LINE = SCROLLBAR_FADE_MESSAGE_COUNT
        + " x android.view.ViewRootImpl$ViewRootHandler android.view.View$ScrollabilityCache";

    private static String stackTraceOf(String methodName, int frameCount) {
        StringBuilder stackTrace = new StringBuilder();
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            if (frameIndex > 0) stackTrace.append('\n');
            stackTrace.append("at ").append(methodName).append(frameIndex)
                .append("(MaterialShapeDrawable.java:").append(1000 + frameIndex).append(')');
        }
        return stackTrace.toString();
    }

    private static DiagnosticsMainThreadStalls stallsOfTheSizeTheDeviceReported() {
        List<MainThreadStallHotPath> hotPaths = new ArrayList<>();
        for (int hotPathIndex = 0; hotPathIndex < 6; hotPathIndex++) {
            hotPaths.add(new MainThreadStallHotPath(
                stackTraceOf("com.google.android.material.shape.ShapePath.path" + hotPathIndex, 11),
                1L, 1571L - hotPathIndex, 1571L - hotPathIndex));
        }
        return new DiagnosticsMainThreadStalls(80L, 32L, 1571L,
            stackTraceOf("com.google.android.material.shape.ShapePath.lineTo", 11), hotPaths, 32L, 0L);
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
        for (int messageIndex = 0; messageIndex < SCROLLBAR_FADE_MESSAGE_COUNT; messageIndex++) {
            dumpLines.add("Message " + messageIndex + ": { when=-1s234ms"
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        for (int messageIndex = 0; messageIndex < 6; messageIndex++) {
            dumpLines.add("Message " + (SCROLLBAR_FADE_MESSAGE_COUNT + messageIndex) + ": { when=-1s234ms"
                + " what=0 target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        dumpLines.add("Message 77: { when=-1s234ms what=0"
            + " target=com.termux.terminal.TerminalSession$MainThreadHandler }");
        return DiagnosticsMainLooperQueue.parse(dumpLines);
    }

    private static ScrollbarViewCensus censusOfTheShapeTheDeviceReported() {
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true),
            new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true),
            new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true),
            new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true),
            new CensusNode("com.termux.view.TerminalView", true),
            new CensusNode("android.widget.ListView", true),
            new CensusNode("androidx.recyclerview.widget.RecyclerView", true)));
    }

    private static List<DiagnosticsSessionLine> sessionLinesThatDeliveredEverything(int sessionCount) {
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

    private static String reportOfTheShapeTheDeviceProduces(DiagnosticsMainThreadStalls stalls) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.3771", 3771, REPORT_MILLIS,
            19, 19, 64, sessionLinesThatDeliveredEverything(19),
            4, 37, true, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(212, 384, 512, 96),
            new DiagnosticsWorkCostLine(27, 28, 6, 4000),
            new DiagnosticsWorkCostLine(7, 16, 5, 4000),
            new DiagnosticsWorkCostLine(6, 39, 21, 4000),
            new DiagnosticsSessionReconnectCost(3, 812, 604, 2, Arrays.asList(
                new DiagnosticsSessionReconnectCostByReason(
                    SessionReconnectReason.SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN, 2, 508, 407),
                new DiagnosticsSessionReconnectCostByReason(
                    SessionReconnectReason.SHELL_PROCESS_EXITED, 1, 304, 304))),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            stalls,
            looperQueueOfTheShapeTheDeviceReported(),
            censusOfTheShapeTheDeviceReported(),
            106000L,
            new DiagnosticsBackgroundCycle(60000L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            new DiagnosticsShellExits(Arrays.asList(
                new DiagnosticsShellExitCount(255, 18),
                new DiagnosticsShellExitCount(0, 2))),
            new DiagnosticsPhantomProcessMonitor("true", 32, true),
            DiagnosticsAppProcessPopulation.measured(49, Arrays.asList(
                new DiagnosticsProcessCommandCount("ssh", 24),
                new DiagnosticsProcessCommandCount("sh", 24),
                new DiagnosticsProcessCommandCount("com.termux", 1))),
            new DiagnosticsWorkCostLine(184, 96, 12, 4000),
            new DiagnosticsWorkCostLine(902, 141, 9, 4000),
            new DiagnosticsSessionCreationPaths(Arrays.asList(
                new DiagnosticsSessionCreationPathCount(SessionCreationPath.RESTORE_OF_A_PERSISTED_SESSION, 19),
                new DiagnosticsSessionCreationPathCount(SessionCreationPath.RECONNECT_OF_A_DEAD_SESSION, 3),
                new DiagnosticsSessionCreationPathCount(SessionCreationPath.NEW_AUTOSSH_SESSION, 2))),
            new DiagnosticsActivityWindows(7, 3),
            DiagnosticsReportDelivery.of("host-0", 11023, 842L, 4096L, 4097L, true));
        return new DiagnosticsReportBuilder().build(report);
    }

    private static String windowTheReportSurvivesIn(String report) {
        return report.substring(0,
            Math.min(report.length(), DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS));
    }

    @Test
    public void theScrollbarFadeCallbackCountSurvivesInAReportOfTheShapeTheDeviceProduces() {
        String report = reportOfTheShapeTheDeviceProduces(stallsOfTheSizeTheDeviceReported());

        Assert.assertTrue("the report has to be longer than the window it survives in, otherwise nothing"
            + " is dropped and this test proves nothing, but it is only " + report.length()
            + " characters. Actual report:\n" + report,
            report.length() > DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);

        String survivingWindow = windowTheReportSurvivesIn(report);
        Assert.assertTrue("the count of pending main-looper messages whose callback fades a scrollbar is"
                + " the one line the freeze investigation reads, and it is emitted after every section"
                + " ahead of it has already taken from the same ceiling, so a section added later takes"
                + " its place without anything saying so. Actual window the report survives in:\n"
                + survivingWindow,
            survivingWindow.contains(SCROLLBAR_FADE_TARGET_LINE));
    }

    @Test
    public void theScrollbarFadeCallbackCountSurvivesHoweverMuchTheStallEvidenceOccupies() {
        String report = reportOfTheShapeTheDeviceProduces(stallsLargeEnoughToFillTheCeilingAlone());

        String survivingWindow = windowTheReportSurvivesIn(report);
        Assert.assertTrue("stall evidence is unbounded in a way the queue measurement is not, so a stall"
                + " large enough to fill the ceiling on its own must lose its own trailing frames rather"
                + " than take the bounded line that names what floods the queue. Actual window the report"
                + " survives in:\n" + survivingWindow,
            survivingWindow.contains(SCROLLBAR_FADE_TARGET_LINE));
    }

    @Test
    public void theCensusOfViewsHoldingAFadeCallbackSurvivesBesideTheCount() {
        String report = reportOfTheShapeTheDeviceProduces(stallsLargeEnoughToFillTheCeilingAlone());

        String survivingWindow = windowTheReportSurvivesIn(report);
        Assert.assertTrue("the count of pending fade callbacks is read against the number of views in the"
                + " live window that can hold one, so a count without that census cannot be turned into a"
                + " conclusion. Actual window the report survives in:\n" + survivingWindow,
            survivingWindow.contains("Views that can hold a scrollbar fade callback"));
        Assert.assertTrue("the total is the number the count is compared against. Actual window the report"
            + " survives in:\n" + survivingWindow, survivingWindow.contains("Total: 7"));
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
