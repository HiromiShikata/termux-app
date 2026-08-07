package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiagnosticsMainLooperQueueTest {

    private static final String LOOPER_HEADER = "Looper (main, tid 2) {b1a2c3}";

    private static String messageLine(int index, String callback, String target) {
        return "  Message " + index + ": { when=+" + index + "ms callback=" + callback
            + " target=" + target + " }";
    }

    private static String whatMessageLine(int index, int what, String target) {
        return "  Message " + index + ": { when=+" + index + "ms what=" + what
            + " target=" + target + " }";
    }

    @Test
    public void anEmptyDumpReportsNoPendingMessages() {
        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList());

        assertEquals(0, queue.getPendingMessageCount());
        assertTrue(queue.getBusiestTargets().isEmpty());
    }

    @Test
    public void headerAndTotalLinesAreNotCountedAsPendingMessages() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            "  (Total messages: 0, polling=true, quitting=false)");

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(0, queue.getPendingMessageCount());
    }

    @Test
    public void everyPendingMessageLineIsCounted() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            messageLine(0, "com.termux.app.SessionSweep", "android.os.Handler"),
            whatMessageLine(1, 3, "android.view.Choreographer$FrameHandler"),
            messageLine(2, "com.termux.app.SessionSweep", "android.os.Handler"),
            "  (Total messages: 3, polling=false, quitting=false)");

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(3, queue.getPendingMessageCount());
    }

    @Test
    public void theBusiestTargetIsReportedFirstWithItsPendingCount() {
        List<String> dumpLines = Arrays.asList(
            LOOPER_HEADER,
            messageLine(0, "com.termux.app.SessionSweep", "android.os.Handler"),
            whatMessageLine(1, 3, "android.view.Choreographer$FrameHandler"),
            messageLine(2, "com.termux.app.SessionSweep", "android.os.Handler"),
            messageLine(3, "com.termux.app.SessionSweep", "android.os.Handler"));

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(dumpLines);

        assertEquals(2, queue.getBusiestTargets().size());
        assertEquals("android.os.Handler com.termux.app.SessionSweep",
            queue.getBusiestTargets().get(0).getDescription());
        assertEquals(3, queue.getBusiestTargets().get(0).getPendingMessageCount());
        assertEquals("android.view.Choreographer$FrameHandler",
            queue.getBusiestTargets().get(1).getDescription());
        assertEquals(1, queue.getBusiestTargets().get(1).getPendingMessageCount());
    }

    @Test
    public void atMostTheConfiguredNumberOfTargetsIsReported() {
        String[] dumpLines = new String[DiagnosticsMainLooperQueue.MAX_REPORTED_TARGETS + 3];
        for (int index = 0; index < dumpLines.length; index++) {
            dumpLines[index] = messageLine(index, "com.termux.app.Work" + index, "android.os.Handler");
        }

        DiagnosticsMainLooperQueue queue = DiagnosticsMainLooperQueue.parse(Arrays.asList(dumpLines));

        assertEquals(dumpLines.length, queue.getPendingMessageCount());
        assertEquals(DiagnosticsMainLooperQueue.MAX_REPORTED_TARGETS, queue.getBusiestTargets().size());
    }
}
