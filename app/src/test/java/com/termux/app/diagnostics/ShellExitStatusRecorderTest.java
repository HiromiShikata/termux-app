package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ShellExitStatusRecorderTest {

    @Test
    public void aRecorderThatSawNoShellExitReportsNoneRatherThanAnUnknownTally() {
        DiagnosticsShellExits shellExits = new ShellExitStatusRecorder().snapshot();

        Assert.assertEquals("a report that cannot distinguish no shell having exited from the exits"
                + " never having been counted leaves the reader unable to tell a healthy application"
                + " from an unmeasured one",
            0, shellExits.getTotalExitCount());
        Assert.assertTrue("no exit was recorded, so naming any exit status would invent an event that"
            + " did not happen", shellExits.getCountsByExitStatus().isEmpty());
    }

    @Test
    public void repeatedExitsWithTheSameStatusAreCountedTogether() {
        ShellExitStatusRecorder recorder = new ShellExitStatusRecorder();
        recorder.recordShellExit(255);
        recorder.recordShellExit(255);
        recorder.recordShellExit(255);

        List<DiagnosticsShellExitCount> countsByExitStatus = recorder.snapshot().getCountsByExitStatus();

        Assert.assertEquals("exits that share a status describe one cause, so they belong on one line",
            1, countsByExitStatus.size());
        Assert.assertEquals(255, countsByExitStatus.get(0).getExitStatus());
        Assert.assertEquals(3, countsByExitStatus.get(0).getCount());
        Assert.assertEquals(3, recorder.snapshot().getTotalExitCount());
    }

    @Test
    public void theStatusBehindTheMostExitsIsReportedFirst() {
        ShellExitStatusRecorder recorder = new ShellExitStatusRecorder();
        recorder.recordShellExit(0);
        recorder.recordShellExit(255);
        recorder.recordShellExit(255);
        recorder.recordShellExit(143);

        List<DiagnosticsShellExitCount> countsByExitStatus = recorder.snapshot().getCountsByExitStatus();

        Assert.assertEquals("the dominant exit status is the one that names the cause worth chasing,"
                + " so it has to be the first thing a reader meets",
            255, countsByExitStatus.get(0).getExitStatus());
        Assert.assertEquals(2, countsByExitStatus.get(0).getCount());
        Assert.assertEquals(4, recorder.snapshot().getTotalExitCount());
    }

    @Test
    public void statusesSeenTheSameNumberOfTimesAreReportedInAStableAscendingOrder() {
        ShellExitStatusRecorder recorder = new ShellExitStatusRecorder();
        recorder.recordShellExit(143);
        recorder.recordShellExit(0);
        recorder.recordShellExit(255);

        List<DiagnosticsShellExitCount> countsByExitStatus = recorder.snapshot().getCountsByExitStatus();

        Assert.assertEquals("an order that depends on the arrival sequence makes two reports of the"
                + " same application state look different", 3, countsByExitStatus.size());
        Assert.assertEquals(0, countsByExitStatus.get(0).getExitStatus());
        Assert.assertEquals(143, countsByExitStatus.get(1).getExitStatus());
        Assert.assertEquals(255, countsByExitStatus.get(2).getExitStatus());
    }
}
