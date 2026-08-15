package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogPlacementResolverTest {

    private static final int AVAILABLE_WIDTH_PIXELS = 1080;
    private static final int AVAILABLE_HEIGHT_PIXELS = 2400;
    private static final int MINIMUM_WIDTH_PIXELS = 270;
    private static final int MINIMUM_HEIGHT_PIXELS = 240;
    private static final OwnerCallDialogPlacement DEFAULT_PLACEMENT =
        new OwnerCallDialogPlacement(0, 300, AVAILABLE_WIDTH_PIXELS, 600);

    @Test
    public void usesTheDefaultPlacementWhileTheOwnerHasNotMovedOrResizedTheDialog() {
        Assert.assertEquals(DEFAULT_PLACEMENT, resolve(null));
    }

    @Test
    public void keepsAPlacementThatAlreadyFitsInsideTheScreen() {
        OwnerCallDialogPlacement requested = new OwnerCallDialogPlacement(120, 800, 700, 500);

        Assert.assertEquals(requested, resolve(requested));
    }

    @Test
    public void pullsAPlacementThatRunsOffTheRightEdgeBackInsideTheScreen() {
        OwnerCallDialogPlacement requested = new OwnerCallDialogPlacement(900, 800, 700, 500);

        Assert.assertEquals(new OwnerCallDialogPlacement(AVAILABLE_WIDTH_PIXELS - 700, 800, 700,
            500), resolve(requested));
    }

    @Test
    public void pullsAPlacementThatRunsOffTheTopEdgeBackInsideTheScreen() {
        OwnerCallDialogPlacement requested = new OwnerCallDialogPlacement(0, 2300, 700, 500);

        Assert.assertEquals(new OwnerCallDialogPlacement(0, AVAILABLE_HEIGHT_PIXELS - 500, 700,
            500), resolve(requested));
    }

    @Test
    public void pullsANegativePlacementBackInsideTheScreen() {
        OwnerCallDialogPlacement requested = new OwnerCallDialogPlacement(-200, -300, 700, 500);

        Assert.assertEquals(new OwnerCallDialogPlacement(0, 0, 700, 500), resolve(requested));
    }

    @Test
    public void refusesToShrinkTheDialogBelowItsMinimumSize() {
        OwnerCallDialogPlacement requested = new OwnerCallDialogPlacement(0, 0, 10, 10);

        Assert.assertEquals(
            new OwnerCallDialogPlacement(0, 0, MINIMUM_WIDTH_PIXELS, MINIMUM_HEIGHT_PIXELS),
            resolve(requested));
    }

    @Test
    public void refusesToGrowTheDialogBeyondTheScreen() {
        OwnerCallDialogPlacement requested =
            new OwnerCallDialogPlacement(0, 0, 9000, 9000);

        Assert.assertEquals(new OwnerCallDialogPlacement(0, 0, AVAILABLE_WIDTH_PIXELS,
            AVAILABLE_HEIGHT_PIXELS), resolve(requested));
    }

    private static OwnerCallDialogPlacement resolve(OwnerCallDialogPlacement requested) {
        return OwnerCallDialogPlacementResolver.resolve(requested, DEFAULT_PLACEMENT,
            AVAILABLE_WIDTH_PIXELS, AVAILABLE_HEIGHT_PIXELS, MINIMUM_WIDTH_PIXELS,
            MINIMUM_HEIGHT_PIXELS);
    }
}
