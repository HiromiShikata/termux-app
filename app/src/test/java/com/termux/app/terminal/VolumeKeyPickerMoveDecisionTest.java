package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VolumeKeyPickerMoveDecisionTest {

    private static final boolean PREVIEW_FIRST = true;
    private static final boolean INSTANT = false;
    private static final boolean OVERLAY_SHOWING = true;
    private static final boolean OVERLAY_HIDDEN = false;
    private static final boolean FORWARD = true;
    private static final boolean BACKWARD = false;

    @Test
    public void instantModeFirstPressMovesAndSwitchesImmediately() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            INSTANT, OVERLAY_HIDDEN, -1, 1, visible, FORWARD);
        Assert.assertEquals(2, decision.getHighlightedSessionIndex());
        Assert.assertTrue(decision.shouldSwitchImmediately());
    }

    @Test
    public void instantModeFirstPressBackwardMovesAndSwitchesImmediately() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            INSTANT, OVERLAY_HIDDEN, -1, 1, visible, BACKWARD);
        Assert.assertEquals(0, decision.getHighlightedSessionIndex());
        Assert.assertTrue(decision.shouldSwitchImmediately());
    }

    @Test
    public void instantModeSubsequentPressKeepsMovingFromHighlightAndSwitches() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            INSTANT, OVERLAY_SHOWING, 2, 1, visible, FORWARD);
        Assert.assertEquals(0, decision.getHighlightedSessionIndex());
        Assert.assertTrue(decision.shouldSwitchImmediately());
    }

    @Test
    public void previewFirstModeFirstPressOnlyAnchorsWithoutSwitching() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            PREVIEW_FIRST, OVERLAY_HIDDEN, -1, 1, visible, FORWARD);
        Assert.assertEquals(1, decision.getHighlightedSessionIndex());
        Assert.assertFalse(decision.shouldSwitchImmediately());
    }

    @Test
    public void previewFirstModeSubsequentPressMovesHighlightWithoutSwitching() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            PREVIEW_FIRST, OVERLAY_SHOWING, 1, 1, visible, FORWARD);
        Assert.assertEquals(2, decision.getHighlightedSessionIndex());
        Assert.assertFalse(decision.shouldSwitchImmediately());
    }

    @Test
    public void instantModeWithNoVisibleSessionsStaysOnCurrentSession() {
        VolumeKeyPickerMoveDecision decision = VolumeKeyPickerMoveDecision.decide(
            INSTANT, OVERLAY_HIDDEN, -1, 4, Collections.emptyList(), FORWARD);
        Assert.assertEquals(4, decision.getHighlightedSessionIndex());
        Assert.assertTrue(decision.shouldSwitchImmediately());
    }
}
