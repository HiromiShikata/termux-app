package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogEdgeGripTest {

    private static final int DIALOG_WIDTH = 1080;
    private static final int DIALOG_HEIGHT = 600;
    private static final int EDGE_BAND = 60;
    private static final int HEADER_BOTTOM = 80;

    private static OwnerCallDialogEdgeGrip gripAt(int touchX, int touchY) {
        return OwnerCallDialogEdgeGrip.resolve(touchX, touchY, DIALOG_WIDTH, DIALOG_HEIGHT,
            EDGE_BAND, HEADER_BOTTOM);
    }

    @Test
    public void theMiddleOfTheDialogGripsNoEdgeSoTheTouchReachesTheBody() {
        Assert.assertFalse(gripAt(DIALOG_WIDTH / 2, DIALOG_HEIGHT / 2).isAnyEdgeGripped());
    }

    @Test
    public void theMiddleOfTheHeaderGripsNoEdgeSoTheHeaderStillMovesTheDialog() {
        Assert.assertFalse(gripAt(DIALOG_WIDTH / 2, HEADER_BOTTOM / 2).isAnyEdgeGripped());
    }

    @Test
    public void theLeftEdgeBelowTheHeaderGripsTheLeftEdgeAlone() {
        OwnerCallDialogEdgeGrip grip = gripAt(EDGE_BAND - 1, DIALOG_HEIGHT / 2);

        Assert.assertTrue(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isRightEdgeGripped());
        Assert.assertFalse(grip.isTopEdgeGripped());
        Assert.assertFalse(grip.isBottomEdgeGripped());
    }

    @Test
    public void theRightEdgeBelowTheHeaderGripsTheRightEdgeAlone() {
        OwnerCallDialogEdgeGrip grip = gripAt(DIALOG_WIDTH - EDGE_BAND + 1, DIALOG_HEIGHT / 2);

        Assert.assertTrue(grip.isRightEdgeGripped());
        Assert.assertFalse(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isTopEdgeGripped());
        Assert.assertFalse(grip.isBottomEdgeGripped());
    }

    @Test
    public void theBottomEdgeGripsTheBottomEdgeAlone() {
        OwnerCallDialogEdgeGrip grip = gripAt(DIALOG_WIDTH / 2, DIALOG_HEIGHT - 1);

        Assert.assertTrue(grip.isBottomEdgeGripped());
        Assert.assertFalse(grip.isTopEdgeGripped());
        Assert.assertFalse(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isRightEdgeGripped());
    }

    @Test
    public void theTopLeftCornerGripsBothEdgesSoTheDialogGrowsUpwardAndSideways() {
        OwnerCallDialogEdgeGrip grip = gripAt(1, 1);

        Assert.assertTrue(grip.isTopEdgeGripped());
        Assert.assertTrue(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isBottomEdgeGripped());
        Assert.assertFalse(grip.isRightEdgeGripped());
    }

    @Test
    public void theTopRightCornerGripsBothEdges() {
        OwnerCallDialogEdgeGrip grip = gripAt(DIALOG_WIDTH - 1, 1);

        Assert.assertTrue(grip.isTopEdgeGripped());
        Assert.assertTrue(grip.isRightEdgeGripped());
    }

    @Test
    public void theBottomRightCornerGripsBothEdges() {
        OwnerCallDialogEdgeGrip grip = gripAt(DIALOG_WIDTH - 1, DIALOG_HEIGHT - 1);

        Assert.assertTrue(grip.isBottomEdgeGripped());
        Assert.assertTrue(grip.isRightEdgeGripped());
    }

    @Test
    public void aTouchOutsideTheDialogGripsNothing() {
        Assert.assertFalse(gripAt(-1, DIALOG_HEIGHT / 2).isAnyEdgeGripped());
        Assert.assertFalse(gripAt(DIALOG_WIDTH + 1, DIALOG_HEIGHT / 2).isAnyEdgeGripped());
        Assert.assertFalse(gripAt(DIALOG_WIDTH / 2, -1).isAnyEdgeGripped());
        Assert.assertFalse(gripAt(DIALOG_WIDTH / 2, DIALOG_HEIGHT + 1).isAnyEdgeGripped());
    }

    @Test
    public void aDialogWithNoSizeYetGripsNothing() {
        Assert.assertFalse(OwnerCallDialogEdgeGrip.resolve(0, 0, 0, 0, EDGE_BAND, HEADER_BOTTOM)
            .isAnyEdgeGripped());
    }

    @Test
    public void aBandWiderThanAThirdOfTheSideLeavesTheMiddleOfThatSideUngripped() {
        OwnerCallDialogEdgeGrip grip = OwnerCallDialogEdgeGrip.resolve(50, 50, 99, 99,
            10_000, 0);

        Assert.assertFalse(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isRightEdgeGripped());
        Assert.assertFalse(grip.isTopEdgeGripped());
        Assert.assertFalse(grip.isBottomEdgeGripped());
    }

    @Test
    public void theBottomRightCornerConstantGripsTheRightAndBottomEdges() {
        OwnerCallDialogEdgeGrip grip = OwnerCallDialogEdgeGrip.bottomRightCorner();

        Assert.assertTrue(grip.isRightEdgeGripped());
        Assert.assertTrue(grip.isBottomEdgeGripped());
        Assert.assertFalse(grip.isLeftEdgeGripped());
        Assert.assertFalse(grip.isTopEdgeGripped());
    }

    @Test
    public void nothingGrippedGripsNoEdge() {
        Assert.assertFalse(OwnerCallDialogEdgeGrip.nothingGripped().isAnyEdgeGripped());
    }
}
