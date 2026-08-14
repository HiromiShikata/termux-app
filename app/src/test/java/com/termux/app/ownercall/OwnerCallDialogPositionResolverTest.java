package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogPositionResolverTest {

    private static final int MIN_BOTTOM_MARGIN = 300;
    private static final int MAX_BOTTOM_MARGIN = 1800;
    private static final int DEFAULT_BOTTOM_MARGIN = 480;

    @Test
    public void returnsTheDefaultPositionWhenNoPositionHasBeenRequested() {
        int resolved = OwnerCallDialogPositionResolver.resolve(null, DEFAULT_BOTTOM_MARGIN,
            MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(DEFAULT_BOTTOM_MARGIN, resolved);
    }

    @Test
    public void usesTheRequestedPositionWhenItFitsInsideTheAllowedArea() {
        int requestedBottomMargin = 900;

        int resolved = OwnerCallDialogPositionResolver.resolve(requestedBottomMargin,
            DEFAULT_BOTTOM_MARGIN, MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(requestedBottomMargin, resolved);
    }

    @Test
    public void clampsToMinimumWhenTheRequestedPositionIsBelowTheAllowedArea() {
        int requestedBottomMargin = MIN_BOTTOM_MARGIN - 1;

        int resolved = OwnerCallDialogPositionResolver.resolve(requestedBottomMargin,
            DEFAULT_BOTTOM_MARGIN, MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(MIN_BOTTOM_MARGIN, resolved);
    }

    @Test
    public void clampsToMaximumWhenTheRequestedPositionIsAboveTheAllowedArea() {
        int requestedBottomMargin = MAX_BOTTOM_MARGIN + 1;

        int resolved = OwnerCallDialogPositionResolver.resolve(requestedBottomMargin,
            DEFAULT_BOTTOM_MARGIN, MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(MAX_BOTTOM_MARGIN, resolved);
    }

    @Test
    public void acceptsExactlyTheMinimumBoundary() {
        int resolved = OwnerCallDialogPositionResolver.resolve(MIN_BOTTOM_MARGIN,
            DEFAULT_BOTTOM_MARGIN, MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(MIN_BOTTOM_MARGIN, resolved);
    }

    @Test
    public void acceptsExactlyTheMaximumBoundary() {
        int resolved = OwnerCallDialogPositionResolver.resolve(MAX_BOTTOM_MARGIN,
            DEFAULT_BOTTOM_MARGIN, MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN);

        Assert.assertEquals(MAX_BOTTOM_MARGIN, resolved);
    }
}
