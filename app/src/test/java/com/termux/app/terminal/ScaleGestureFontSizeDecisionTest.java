package com.termux.app.terminal;

import com.termux.app.terminal.ScaleGestureFontSizeDecision.Action;

import org.junit.Assert;
import org.junit.Test;

public class ScaleGestureFontSizeDecisionTest {

    @Test
    public void noChangeWithinDeadZone() {
        Assert.assertEquals(Action.NO_CHANGE, ScaleGestureFontSizeDecision.decide(1.0f));
        Assert.assertEquals(Action.NO_CHANGE, ScaleGestureFontSizeDecision.decide(0.9f));
        Assert.assertEquals(Action.NO_CHANGE, ScaleGestureFontSizeDecision.decide(1.1f));
        Assert.assertEquals(Action.NO_CHANGE, ScaleGestureFontSizeDecision.decide(0.95f));
        Assert.assertEquals(Action.NO_CHANGE, ScaleGestureFontSizeDecision.decide(1.05f));
    }

    @Test
    public void increaseWhenScaleAboveUpperThreshold() {
        Assert.assertEquals(Action.INCREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(1.2f));
        Assert.assertEquals(Action.INCREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(2.0f));
    }

    @Test
    public void decreaseWhenScaleBelowLowerThreshold() {
        Assert.assertEquals(Action.DECREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(0.8f));
        Assert.assertEquals(Action.DECREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(0.5f));
    }

    @Test
    public void justOutsideUpperThresholdIncreases() {
        Assert.assertEquals(Action.INCREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(1.1001f));
    }

    @Test
    public void justOutsideLowerThresholdDecreases() {
        Assert.assertEquals(Action.DECREASE_FONT_SIZE, ScaleGestureFontSizeDecision.decide(0.8999f));
    }
}
