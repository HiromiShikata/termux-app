package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class ActivityWindowRecorderTest {

    @Test
    public void aProcessThatHasNotBuiltTheActivityWindowReportsNothingBuilt() {
        DiagnosticsActivityWindows windows = new ActivityWindowRecorder().snapshot();

        Assert.assertEquals("the activity window has not been built yet", 0, windows.getCreatedCount());
        Assert.assertEquals("nothing has been torn down yet", 0, windows.getDestroyedCount());
        Assert.assertEquals("no teardown can be outstanding before anything was built",
            0, windows.getTeardownNotRunCount());
    }

    @Test
    public void anActivityWindowThatWasBuiltAndNotTornDownHasItsTeardownOutstanding() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordActivityCreated();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals(1, windows.getCreatedCount());
        Assert.assertEquals(0, windows.getDestroyedCount());
        Assert.assertEquals("the window on screen has been built and not torn down",
            1, windows.getTeardownNotRunCount());
    }

    @Test
    public void anActivityWindowThatWasBuiltAndTornDownLeavesNoTeardownOutstanding() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordActivityCreated();
        recorder.recordActivityDestroyed();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals(1, windows.getCreatedCount());
        Assert.assertEquals(1, windows.getDestroyedCount());
        Assert.assertEquals(0, windows.getTeardownNotRunCount());
    }

    @Test
    public void rebuildingTheActivityWindowRepeatedlyIsCountedEveryTime() {
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        for (int build = 0; build < 10; build++) {
            recorder.recordActivityCreated();
        }
        recorder.recordActivityDestroyed();

        DiagnosticsActivityWindows windows = recorder.snapshot();

        Assert.assertEquals("every build costs the inflation and first draw of the whole view tree,"
                + " so the number of builds is the reading that matters", 10, windows.getCreatedCount());
        Assert.assertEquals(1, windows.getDestroyedCount());
        Assert.assertEquals("nine builds have not had their teardown run",
            9, windows.getTeardownNotRunCount());
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
