package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionListBottomSheetHorizontalBoundsTest {

    @Test
    public void portraitUsesFullContainerWidth() {
        Assert.assertEquals(
            SessionListBottomSheetHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionListBottomSheetHorizontalBounds.resolveWidthPixels(false, 1080));
    }

    @Test
    public void landscapeUsesFullContainerWidth() {
        Assert.assertEquals(
            SessionListBottomSheetHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionListBottomSheetHorizontalBounds.resolveWidthPixels(true, 1920));
    }

    @Test
    public void landscapeUsesFullWidthWhenContainerWidthUnknown() {
        Assert.assertEquals(
            SessionListBottomSheetHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionListBottomSheetHorizontalBounds.resolveWidthPixels(true, 0));
    }

    @Test
    public void landscapeUsesFullWidthWhenContainerWidthNegative() {
        Assert.assertEquals(
            SessionListBottomSheetHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionListBottomSheetHorizontalBounds.resolveWidthPixels(true, -5));
    }

    @Test
    public void portraitDoesNotAlignToEnd() {
        Assert.assertFalse(SessionListBottomSheetHorizontalBounds.alignToEnd(false));
    }

    @Test
    public void landscapeDoesNotAlignToEnd() {
        Assert.assertFalse(SessionListBottomSheetHorizontalBounds.alignToEnd(true));
    }
}
