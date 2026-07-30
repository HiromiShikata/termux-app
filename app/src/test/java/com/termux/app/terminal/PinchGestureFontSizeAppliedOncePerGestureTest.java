package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = {"com.termux.view"}, shadows = {TextSizeApplicationRecordingShadowTerminalView.class})
public class PinchGestureFontSizeAppliedOncePerGestureTest {

    private PinchGestureFontSizeTestHarness harness;

    @Before
    public void setUp() throws Exception {
        harness = new PinchGestureFontSizeTestHarness(RuntimeEnvironment.getApplication());
    }

    @Test
    public void aPinchGestureOfThreeStepsAppliesTheSizeOfAllThreeStepsExactlyOnce() {
        int fontSizeBeforeGesture = harness.preferences.getFontSize();

        harness.pinchApart(3);
        harness.client.onScaleEnd();

        Assert.assertEquals("the whole gesture must pay the renderer rebuild and transcript reflow once rather "
                + "than once per crossed threshold, and the size it applies must be the one the per step logic "
                + "arrives at", Collections.singletonList(fontSizeBeforeGesture + 6),
            harness.appliedTextSizes());
    }

    @Test
    public void aPinchGestureBelowTheThresholdAppliesNothingWhenItEnds() {
        int fontSizeBeforeGesture = harness.preferences.getFontSize();

        harness.client.onScale(1.05f);
        harness.client.onScale(0.97f);
        harness.client.onScaleEnd();

        Assert.assertEquals("a gesture that decided no step has nothing to apply, so ending it must not trigger "
                + "a transcript reflow", Collections.emptyList(), harness.appliedTextSizes());
        Assert.assertEquals("a gesture that decided no step must leave the stored font size untouched",
            fontSizeBeforeGesture, harness.preferences.getFontSize());
    }

    @Test
    public void aPinchGestureRunningPastTheMaximumAppliesTheClampedMaximumOnce() {
        int maximumFontSize = harness.maximumFontSize();
        harness.preferences.setFontSize(maximumFontSize - 2);

        harness.pinchApart(4);
        harness.client.onScaleEnd();

        Assert.assertEquals("coalescing the steps of a gesture must not bypass the clamp, because the size that "
                + "finally reflows the transcript has to be the same size the per step logic would have reached",
            Collections.singletonList(maximumFontSize), harness.appliedTextSizes());
    }

    @Test
    public void aPinchGestureRunningPastTheMinimumAppliesTheClampedMinimumOnce() {
        int minimumFontSize = harness.minimumFontSize();
        harness.preferences.setFontSize(minimumFontSize + 2);

        harness.pinchTogether(4);
        harness.client.onScaleEnd();

        Assert.assertEquals("coalescing the steps of a gesture must not bypass the clamp, because the size that "
                + "finally reflows the transcript has to be the same size the per step logic would have reached",
            Collections.singletonList(minimumFontSize), harness.appliedTextSizes());
    }

    @Test
    public void aSecondPinchGestureStartsFromTheSizeTheFirstGestureProduced() {
        int fontSizeBeforeFirstGesture = harness.preferences.getFontSize();

        harness.pinchApart(2);
        harness.client.onScaleEnd();
        harness.pinchApart(1);
        harness.client.onScaleEnd();

        Assert.assertEquals("deferring the reflow to the end of a gesture must not lose the steps of the gesture "
                + "before it, so the second gesture pays one reflow that starts from the size the first one left",
            Arrays.asList(fontSizeBeforeFirstGesture + 4, fontSizeBeforeFirstGesture + 6),
            harness.appliedTextSizes());
    }
}
