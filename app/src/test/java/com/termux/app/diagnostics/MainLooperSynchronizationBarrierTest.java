package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainLooperSynchronizationBarrierTest {

    private static final String LOOPER_HEADER = "Looper (main, tid 2) {b1a2c3}";

    private static String barrierLine(int index, String due, int token) {
        return "  Message " + index + ": { when=" + due + " barrier=" + token + " }";
    }

    private static String messageLine(int index, String due, String callback, String target) {
        return "  Message " + index + ": { when=" + due + " callback=" + callback
            + " target=" + target + " }";
    }

    @Test
    public void aQueueHoldingNoBarrierSaysSoRatherThanLeavingTheReaderToInferIt() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            messageLine(0, "+4ms", "com.termux.app.SessionSweep", "android.os.Handler"));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals("a queue with no barrier is the healthy case and has to be distinguishable from a"
            + " reading that never looked", 0, queue.getSynchronizationBarrierCount());
        assertEquals(0, queue.getMessageCountBehindFirstSynchronizationBarrier());
        assertEquals("", queue.getFirstSynchronizationBarrierDueDescription());
    }

    @Test
    public void aBarrierAtTheHeadIsCountedWithEverythingQueuedBehindIt() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            barrierLine(0, "-11ms", 2372),
            messageLine(1, "-4ms", "com.termux.app.TermuxService$ExternalSyntheticLambda7",
                "android.os.Handler"),
            messageLine(2, "+425ms", "android.view.View$ScrollabilityCache",
                "android.view.ViewRootImpl$ViewRootHandler"));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals("a queue entry the platform prints with a barrier token and no target is a"
            + " synchronization barrier, and while it sits at the head the looper runs no synchronous"
            + " message behind it", 1, queue.getSynchronizationBarrierCount());
        assertEquals("the number of messages the barrier is holding is what says how much work the"
            + " interface owes the moment it is lifted", 2,
            queue.getMessageCountBehindFirstSynchronizationBarrier());
        assertEquals("a barrier already past due is one the traversal that posted it never came back to"
            + " remove", "-11ms", queue.getFirstSynchronizationBarrierDueDescription());
    }

    @Test
    public void aBarrierIsNamedRatherThanCountedAsAnUnknownTarget() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            barrierLine(0, "-11ms", 2372));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(1, queue.getBusiestTargets().size());
        assertEquals("a barrier listed as an unknown target reads as an unattributed message and hides"
            + " the one entry that explains a frozen interface", "synchronization barrier",
            queue.getBusiestTargets().get(0).getDescription());
    }

    @Test
    public void everyMessageBehindTheBarrierIsCountedBeyondTheKeptLineCap() {
        List<String> dumpLines = new ArrayList<>();
        dumpLines.add(LOOPER_HEADER);
        dumpLines.add(barrierLine(0, "-11ms", 2372));
        int messagesBehindTheBarrier = DiagnosticsMainLooperQueue.MAX_REPORTED_MESSAGE_LINES + 26;
        for (int messageIndex = 1; messageIndex <= messagesBehindTheBarrier; messageIndex++) {
            dumpLines.add(messageLine(messageIndex, "+425ms", "android.view.View$ScrollabilityCache",
                "android.view.ViewRootImpl$ViewRootHandler"));
        }

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals("the raw dump keeps only the first lines, so a count taken from those lines would"
            + " understate what the barrier is holding", messagesBehindTheBarrier,
            queue.getMessageCountBehindFirstSynchronizationBarrier());
    }

    @Test
    public void aBarrierBehindOtherMessagesHoldsOnlyWhatIsQueuedAfterIt() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            messageLine(0, "-9ms", "com.termux.app.SessionSweep", "android.os.Handler"),
            barrierLine(1, "-2ms", 2372),
            messageLine(2, "+425ms", "android.view.View$ScrollabilityCache",
                "android.view.ViewRootImpl$ViewRootHandler"));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(1, queue.getSynchronizationBarrierCount());
        assertEquals("a barrier the looper has not reached yet is described by its own due time, not by"
            + " the age of the queue in front of it", "-2ms",
            queue.getFirstSynchronizationBarrierDueDescription());
        assertEquals("the messages ahead of a barrier still run, so counting them as held would overstate"
            + " what the interface owes", 1,
            queue.getMessageCountBehindFirstSynchronizationBarrier());
    }

    @Test
    public void aSecondBarrierIsCountedWhileTheDueTimeStaysThatOfTheOldest() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            barrierLine(0, "-11ms", 2372),
            messageLine(1, "-4ms", "com.termux.app.TermuxService$ExternalSyntheticLambda7",
                "android.os.Handler"),
            barrierLine(2, "+2ms", 2373));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(2, queue.getSynchronizationBarrierCount());
        assertEquals("the head of the queue is the barrier that is actually holding the looper", "-11ms",
            queue.getFirstSynchronizationBarrierDueDescription());
        assertEquals(2, queue.getMessageCountBehindFirstSynchronizationBarrier());
    }
}
