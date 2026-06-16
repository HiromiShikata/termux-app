package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VolumeKeyPickerStepTest {

    private static final boolean OVERLAY_SHOWING = true;
    private static final boolean OVERLAY_HIDDEN = false;
    private static final boolean FORWARD = true;
    private static final boolean BACKWARD = false;

    @Test
    public void firstPressAnchorsHighlightOnCurrentSessionWithoutMovingForward() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        Assert.assertEquals(1, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_HIDDEN, -1, 1, visible, FORWARD));
    }

    @Test
    public void firstPressAnchorsHighlightOnCurrentSessionWithoutMovingBackward() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        Assert.assertEquals(1, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_HIDDEN, -1, 1, visible, BACKWARD));
    }

    @Test
    public void firstPressSnapsToNearestVisibleSessionWhenCurrentSessionIsNotVisible() {
        List<Integer> visible = Arrays.asList(0, 3, 5);
        Assert.assertEquals(3, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_HIDDEN, -1, 2, visible, FORWARD));
    }

    @Test
    public void subsequentPressMovesHighlightToNextVisibleSession() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        Assert.assertEquals(2, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 1, 1, visible, FORWARD));
    }

    @Test
    public void subsequentPressMovesHighlightToPreviousVisibleSession() {
        List<Integer> visible = Arrays.asList(0, 1, 2);
        Assert.assertEquals(0, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 1, 1, visible, BACKWARD));
    }

    @Test
    public void subsequentPressSkipsSessionsInsideCollapsedProjectGroups() {
        List<Integer> visibleAfterCollapse = Arrays.asList(0, 3, 5);
        Assert.assertEquals(3, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 0, 0, visibleAfterCollapse, FORWARD));
        Assert.assertEquals(5, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 3, 0, visibleAfterCollapse, FORWARD));
    }

    @Test
    public void subsequentPressWrapsAroundForwardFromLastToFirstVisibleSession() {
        List<Integer> visible = Arrays.asList(0, 3, 5);
        Assert.assertEquals(0, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 5, 0, visible, FORWARD));
    }

    @Test
    public void subsequentPressWrapsAroundBackwardFromFirstToLastVisibleSession() {
        List<Integer> visible = Arrays.asList(0, 3, 5);
        Assert.assertEquals(5, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 0, 0, visible, BACKWARD));
    }

    @Test
    public void returnsCurrentSessionIndexWhenNoVisibleSessionsExist() {
        Assert.assertEquals(4, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_HIDDEN, -1, 4, Collections.emptyList(), FORWARD));
        Assert.assertEquals(4, VolumeKeyPickerStep.nextHighlightedSessionIndex(
            OVERLAY_SHOWING, 4, 4, Collections.emptyList(), FORWARD));
    }
}
