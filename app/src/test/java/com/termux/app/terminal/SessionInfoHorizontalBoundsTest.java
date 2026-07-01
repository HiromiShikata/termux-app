package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionInfoHorizontalBoundsTest {

    @Test
    public void portraitUsesFullWidth() {
        Assert.assertEquals(
            SessionInfoHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionInfoHorizontalBounds.resolveWidthPixels(false, 720));
    }

    @Test
    public void landscapeUsesBrowserColumnWidth() {
        Assert.assertEquals(
            960,
            SessionInfoHorizontalBounds.resolveWidthPixels(true, 960));
    }

    @Test
    public void landscapeUsesFullWidthWhenBrowserColumnWidthUnknown() {
        Assert.assertEquals(
            SessionInfoHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionInfoHorizontalBounds.resolveWidthPixels(true, 0));
    }

    @Test
    public void landscapeUsesFullWidthWhenBrowserColumnWidthNegative() {
        Assert.assertEquals(
            SessionInfoHorizontalBounds.MATCH_PARENT_WIDTH,
            SessionInfoHorizontalBounds.resolveWidthPixels(true, -3));
    }
}
