package com.termux.app.diagnostics;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.ref.WeakReference;

@RunWith(RobolectricTestRunner.class)
public class ScrollbarViewCensusOverEveryRecordedWindowTest {

    private static View windowRootHoldingOneScrollableView(Context context) {
        ViewGroup windowRoot = new FrameLayout(context);
        View scrollableView = new View(context);
        scrollableView.setVerticalScrollBarEnabled(true);
        scrollableView.setScrollbarFadingEnabled(true);
        windowRoot.addView(scrollableView);
        return windowRoot;
    }

    @Test
    public void theCensusWalksEveryWindowTheProcessStillHolds() {
        Context context = RuntimeEnvironment.getApplication();
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordWindowRoot(windowRootHoldingOneScrollableView(context));
        recorder.recordWindowRoot(windowRootHoldingOneScrollableView(context));

        ScrollbarViewCensus census = ScrollbarViewCensusSnapshot.take(recorder.snapshotWindowRoots());

        Assert.assertEquals("the pending fade callbacks are posted by every view in the process, so a"
                + " census that stops at the first window states a number that cannot account for them",
            2, census.getWindowCount());
        Assert.assertEquals("each recorded window holds one view that can hold a fade callback",
            2, census.getScrollbarViewCount());
    }

    @Test
    public void aWindowRecordedTwiceIsWalkedOnce() {
        Context context = RuntimeEnvironment.getApplication();
        View windowRoot = windowRootHoldingOneScrollableView(context);
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordWindowRoot(windowRoot);
        recorder.recordWindowRoot(windowRoot);

        ScrollbarViewCensus census = ScrollbarViewCensusSnapshot.take(recorder.snapshotWindowRoots());

        Assert.assertEquals("counting one window twice would inflate the total by exactly the amount the"
            + " reading exists to measure", 1, census.getWindowCount());
        Assert.assertEquals(1, census.getScrollbarViewCount());
    }

    @Test
    public void aWindowTheProcessHasReleasedIsReportedRatherThanDroppedFromTheReading() {
        Context context = RuntimeEnvironment.getApplication();
        ActivityWindowRecorder recorder = new ActivityWindowRecorder();
        recorder.recordWindowRoot(windowRootHoldingOneScrollableView(context));
        WeakReference<View> releasedWindowRoot =
            new WeakReference<>(windowRootHoldingOneScrollableView(context));
        recorder.recordWindowRootReference(releasedWindowRoot);
        releasedWindowRoot.clear();

        ScrollbarViewCensus census = ScrollbarViewCensusSnapshot.take(recorder.snapshotWindowRoots());

        Assert.assertEquals("a window whose views the census cannot reach is exactly the case the reading"
                + " must not pass over in silence", 1, census.getWindowsNoLongerReachableCount());
        Assert.assertEquals(1, census.getWindowCount());
    }
}
