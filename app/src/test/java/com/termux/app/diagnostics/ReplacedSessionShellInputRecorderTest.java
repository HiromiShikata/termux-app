package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class ReplacedSessionShellInputRecorderTest {

    @Test
    public void aReplacementThatLeftNothingBehindIsNotCountedAsAnEpisode() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced("host-healthy", 0L, true, null);

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals("an ordinary replacement of a session that wrote everything it accepted is not"
            + " an episode, and counting it would bury the real ones",
            0, snapshot.getSessionsReplacedWithInputUndelivered());
        Assert.assertEquals(0L, snapshot.getWorstUndeliveredBytes());
        Assert.assertEquals("", snapshot.getWorstUndeliveredSessionName());
    }

    @Test
    public void theLargestLossIsKeptRatherThanTheMostRecentOne() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced("host-small", 40L, true, null);
        recorder.recordSessionBeingReplaced("host-large", 3096L, true, null);
        recorder.recordSessionBeingReplaced("host-later-but-smaller", 12L, true, null);

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals(3, snapshot.getSessionsReplacedWithInputUndelivered());
        Assert.assertEquals("the worst loss is the one that explains the symptom, and a later smaller one"
            + " must not overwrite it", 3096L, snapshot.getWorstUndeliveredBytes());
        Assert.assertEquals("host-large", snapshot.getWorstUndeliveredSessionName());
    }

    @Test
    public void aWriterThatStoppedIsRecordedEvenWithNothingLeftUnwritten() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced("host-dead", 0L, false, "broken pipe");

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals("a writer that stopped is why later input would go nowhere, so the replacement"
            + " counts even though nothing was outstanding at that instant",
            1, snapshot.getSessionsReplacedWithInputUndelivered());
        Assert.assertEquals("host-dead", snapshot.getLastWriterStopSessionName());
        Assert.assertEquals("broken pipe", snapshot.getLastWriterStopReason());
    }

    @Test
    public void theMostRecentWriterStopIsKeptSoTheLatestFailureIsTheOneReported() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced("host-first", 10L, false, "broken pipe");
        recorder.recordSessionBeingReplaced("host-second", 5L, false, "shell exited");

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals("host-second", snapshot.getLastWriterStopSessionName());
        Assert.assertEquals("shell exited", snapshot.getLastWriterStopReason());
    }

    @Test
    public void aRunningWriterLeavesTheLastWriterStopUntouched() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced("host-dead", 10L, false, "broken pipe");
        recorder.recordSessionBeingReplaced("host-slow", 99L, true, null);

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals("a replacement whose writer was still running is not a writer stop and must not"
            + " overwrite the real one", "host-dead", snapshot.getLastWriterStopSessionName());
        Assert.assertEquals("broken pipe", snapshot.getLastWriterStopReason());
    }

    @Test
    public void anUnnamedSessionIsRecordedWithoutAName() {
        ReplacedSessionShellInputRecorder recorder = new ReplacedSessionShellInputRecorder();

        recorder.recordSessionBeingReplaced(null, 7L, false, null);

        DiagnosticsReplacedSessionShellInput snapshot = recorder.snapshot();
        Assert.assertEquals(1, snapshot.getSessionsReplacedWithInputUndelivered());
        Assert.assertEquals("", snapshot.getWorstUndeliveredSessionName());
        Assert.assertEquals("", snapshot.getLastWriterStopReason());
    }
}
