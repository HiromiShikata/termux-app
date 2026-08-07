package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = {"com.termux.view"}, shadows = {TextSizeApplicationRecordingShadowTerminalView.class})
public class PinchGestureFontSizeReflowDeferralTest {

    private PinchGestureFontSizeTestHarness harness;

    @Before
    public void setUp() throws Exception {
        harness = new PinchGestureFontSizeTestHarness(RuntimeEnvironment.getApplication());
    }

    @Test
    public void aPinchGestureCrossingTheThresholdSeveralTimesReflowsNothingWhileTheFingersAreStillMoving() {
        harness.pinchApart(3);

        Assert.assertEquals("every applied font size rebuilds the renderer and, when the column count changes, "
                + "reflows the whole accumulated scrollback on the main thread, so the steps of one pinch "
                + "gesture must not each pay that cost while the fingers are still moving; the applied sizes "
                + "recorded during the gesture were " + harness.appliedTextSizes(),
            0, harness.appliedTextSizes().size());
    }

    @Test
    public void aPinchGestureThatOnlyShrinksTheFontReflowsNothingWhileTheFingersAreStillMoving() {
        harness.pinchTogether(3);

        Assert.assertEquals("shrinking pinches cross the same threshold repeatedly and each applied size pays "
                + "the same transcript reflow, so no font size may be applied before the gesture ends; the "
                + "applied sizes recorded during the gesture were " + harness.appliedTextSizes(),
            0, harness.appliedTextSizes().size());
    }

    @Test
    public void aPinchGestureStayingInsideTheDeadZoneNeitherReflowsNorMovesTheFontSize() {
        int fontSizeBeforeGesture = harness.preferences.getFontSize();

        harness.client.onScale(1.05f);
        harness.client.onScale(0.95f);
        harness.client.onScale(1.02f);

        Assert.assertEquals("a gesture that never crosses the ten percent threshold decides no step at all, so "
                + "it must not pay a single transcript reflow", 0, harness.appliedTextSizes().size());
        Assert.assertEquals("a gesture that never crosses the ten percent threshold must leave the stored font "
                + "size untouched, exactly as before this change", fontSizeBeforeGesture,
            harness.preferences.getFontSize());
    }
}
