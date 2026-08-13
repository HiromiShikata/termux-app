package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class ActivityWindowRecorderTest {

    @Test
    public void aProcessThatHasNotCreatedAnActivityReportsNothingOutstanding() {
        DiagnosticsActivityWindows windows = new ActivityWindowRecorder().snapshot();

        Assert.assertEquals("no activity has been created yet", 0, windows.getCreatedCount());
        Assert.assertEquals("no activity has been destroyed yet", 0, windows.getDestroyedCount());
        Assert.assertEquals("nothing can be outstanding before anything was created",
            0, windows.getUndestroyedCount());
    }

    @Test
    public void anActivityThatWasCreatedAndNotDestroyedIsCountedAsOutstanding() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordActivityCreated();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals(1, windows.getCreatedCount());
        Assert.assertEquals(0, windows.getDestroyedCount());
        Assert.assertEquals("the created activity has not been destroyed",
            1, windows.getUndestroyedCount());
    }

    @Test
    public void anActivityThatWasCreatedAndDestroyedLeavesNothingOutstanding() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordActivityCreated();
        recorder.recordActivityDestroyed();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals(1, windows.getCreatedCount());
        Assert.assertEquals(1, windows.getDestroyedCount());
        Assert.assertEquals(0, windows.getUndestroyedCount());
    }

    @Test
    public void recreatingTheActivityRepeatedlyWithoutDestroyingLeavesEveryOneOutstanding() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        for (int creation = 0; creation < 10; creation++) {
            recorder.recordActivityCreated();
        }
        recorder.recordActivityDestroyed();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals(10, windows.getCreatedCount());
        Assert.assertEquals(1, windows.getDestroyedCount());
        Assert.assertEquals("nine activities were created and never destroyed",
            9, windows.getUndestroyedCount());
    }

    @Test
    public void aSnapshotDoesNotChangeWhenTheRecorderKeepsCounting() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordActivityCreated();

        DiagnosticsActivityWindows takenEarlier = recorder.snapshot();
        recorder.recordActivityCreated();

        Assert.assertEquals("the earlier snapshot has to keep the value it was taken with",
            1, takenEarlier.getCreatedCount());
        Assert.assertEquals(2, recorder.snapshot().getCreatedCount());
    }
}
