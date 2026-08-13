package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainLooperQueuePeakRecorderTest {

    private static final long FLOOD_MILLIS = 1783216800000L;

    private static final long AFTER_THE_FLOOD_MILLIS = FLOOD_MILLIS + 60000L;

    private static final class CountingCensusSource implements MainLooperQueuePeakRecorder.ScrollbarViewCensusSource {

        private final int mScrollbarViewCount;

        private int mWalkCount;

        CountingCensusSource(int scrollbarViewCount) {
            mScrollbarViewCount = scrollbarViewCount;
        }

        @Override
        public ScrollbarViewCensus takeScrollbarViewCensus() {
            mWalkCount++;
            return censusOf(mScrollbarViewCount);
        }

        int getWalkCount() {
            return mWalkCount;
        }
    }

    private static final class CensusNode implements ScrollbarViewCensus.ViewNode {

        private final String mClassName;
        private final boolean mCanHoldScrollbarFadeCallback;
        private final List<ScrollbarViewCensus.ViewNode> mChildren;

        CensusNode(String className, boolean canHoldScrollbarFadeCallback,
                   ScrollbarViewCensus.ViewNode... children) {
            mClassName = className;
            mCanHoldScrollbarFadeCallback = canHoldScrollbarFadeCallback;
            mChildren = new ArrayList<>(Arrays.asList(children));
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

    private static ScrollbarViewCensus censusOf(int scrollbarViewCount) {
        List<ScrollbarViewCensus.ViewNode> children = new ArrayList<>();
        for (int viewIndex = 0; viewIndex < scrollbarViewCount; viewIndex++) {
            children.add(new CensusNode("com.termux.app.browser.BrowserAssistStructureFreeWebView", true));
        }
        return ScrollbarViewCensus.take(new CensusNode("android.widget.FrameLayout", false,
            children.toArray(new ScrollbarViewCensus.ViewNode[0])));
    }

    private static DiagnosticsMainLooperQueue queueOfScrollbarFadeCallbacks(int pendingMessageCount) {
        List<String> dumpLines = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < pendingMessageCount; messageIndex++) {
            dumpLines.add("Message " + messageIndex + ": { when=-1s234ms"
                + " callback=android.view.View$ScrollabilityCache"
                + " target=android.view.ViewRootImpl$ViewRootHandler }");
        }
        return DiagnosticsMainLooperQueue.parse(dumpLines);
    }

    private static DiagnosticsMainLooperQueue queueOfTerminalMessages(int pendingMessageCount) {
        List<String> dumpLines = new ArrayList<>();
        for (int messageIndex = 0; messageIndex < pendingMessageCount; messageIndex++) {
            dumpLines.add("Message " + messageIndex + ": { when=-1s234ms what=0"
                + " target=com.termux.terminal.TerminalSession$MainThreadHandler }");
        }
        return DiagnosticsMainLooperQueue.parse(dumpLines);
    }

    @Test
    public void aFloodThatHasDrainedBeforeTheReportIsCollectedIsStillReadableAfterwards() {
        MainLooperQueuePeakRecorder recorder = new MainLooperQueuePeakRecorder();

        recorder.recordObservation(queueOfScrollbarFadeCallbacks(82), FLOOD_MILLIS,
            new CountingCensusSource(74));
        recorder.recordObservation(queueOfTerminalMessages(4), AFTER_THE_FLOOD_MILLIS,
            new CountingCensusSource(7));

        DiagnosticsMainLooperQueuePeak peak = recorder.snapshot();
        Assert.assertTrue("a recorder that has taken an observation has to say so, because a peak that"
            + " reads as never sampled cannot be told apart from a queue that stayed empty",
            peak.wasObserved());
        Assert.assertEquals("the freeze is the moment the queue was deepest, and a report collected after"
                + " it drained states four pending messages, so without the highest observation the"
                + " episode leaves no trace at all",
            82, peak.getPendingMessageCount());
        Assert.assertEquals("the time of the deepest queue is what the reading is lined up against the"
            + " owner's report of the freeze with", FLOOD_MILLIS, peak.getObservedAtMillis());
    }

    @Test
    public void theBusiestTargetsAndTheViewCensusOfTheHighestObservationAreKept() {
        MainLooperQueuePeakRecorder recorder = new MainLooperQueuePeakRecorder();

        recorder.recordObservation(queueOfScrollbarFadeCallbacks(82), FLOOD_MILLIS,
            new CountingCensusSource(74));
        recorder.recordObservation(queueOfTerminalMessages(4), AFTER_THE_FLOOD_MILLIS,
            new CountingCensusSource(7));

        DiagnosticsMainLooperQueuePeak peak = recorder.snapshot();
        Assert.assertEquals("a count with nothing behind it cannot be attributed to what posted the"
                + " messages, so the busiest targets of that same observation have to travel with it",
            1, peak.getBusiestTargets().size());
        Assert.assertEquals("the target of the deepest observation is the scrollbar fade callback, not the"
                + " terminal messages that were pending when the report was collected",
            "android.view.ViewRootImpl$ViewRootHandler android.view.View$ScrollabilityCache",
            peak.getBusiestTargets().get(0).getDescription());
        Assert.assertEquals("the pending count is compared against how many views could each hold one such"
                + " callback, and that comparison is only meaningful for the same moment",
            74, peak.getScrollbarViewCensus().getScrollbarViewCount());
    }

    @Test
    public void theViewTreeIsWalkedOnlyWhenTheObservationBeatsTheHighestSoFar() {
        MainLooperQueuePeakRecorder recorder = new MainLooperQueuePeakRecorder();
        CountingCensusSource floodCensusSource = new CountingCensusSource(74);
        CountingCensusSource quietCensusSource = new CountingCensusSource(7);

        recorder.recordObservation(queueOfScrollbarFadeCallbacks(82), FLOOD_MILLIS, floodCensusSource);
        recorder.recordObservation(queueOfTerminalMessages(4), AFTER_THE_FLOOD_MILLIS, quietCensusSource);
        recorder.recordObservation(queueOfTerminalMessages(4), AFTER_THE_FLOOD_MILLIS + 60000L,
            quietCensusSource);

        Assert.assertEquals("the walk over the view tree is the expensive half of the sampling, so an"
                + " observation that does not beat the highest so far must not pay for it",
            0, quietCensusSource.getWalkCount());
        Assert.assertEquals("the observation that does beat it has to pay for it once, because the census"
            + " is what the count is attributed against", 1, floodCensusSource.getWalkCount());
    }

    @Test
    public void anEqualObservationDoesNotDisplaceTheEarlierTimeTheDepthWasFirstReached() {
        MainLooperQueuePeakRecorder recorder = new MainLooperQueuePeakRecorder();

        recorder.recordObservation(queueOfScrollbarFadeCallbacks(82), FLOOD_MILLIS,
            new CountingCensusSource(74));
        recorder.recordObservation(queueOfScrollbarFadeCallbacks(82), AFTER_THE_FLOOD_MILLIS,
            new CountingCensusSource(74));

        Assert.assertEquals("the first time the queue reached its deepest is the moment the owner's report"
                + " of the freeze is lined up against, and a later observation of the same depth would"
                + " move it away from that moment", FLOOD_MILLIS, recorder.snapshot().getObservedAtMillis());
    }

    @Test
    public void anEmptyQueueIsRecordedRatherThanLeavingThePeakUnsampled() {
        MainLooperQueuePeakRecorder recorder = new MainLooperQueuePeakRecorder();

        recorder.recordObservation(queueOfTerminalMessages(0), FLOOD_MILLIS, new CountingCensusSource(7));

        DiagnosticsMainLooperQueuePeak peak = recorder.snapshot();
        Assert.assertTrue("a report that says the peak was never sampled while sampling has been running"
                + " for hours would send the investigation after the sampler rather than the freeze",
            peak.wasObserved());
        Assert.assertEquals("an empty queue is a measured zero, which is itself evidence that nothing"
            + " accumulated", 0, peak.getPendingMessageCount());
    }
}
