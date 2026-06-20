package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserRenderedFrameOwnershipTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";
    private static final String URL_X = "https://example.com/x";
    private static final String URL_Y = "https://example.com/y";

    @Test
    public void renderedFrameOwnedByCurrentSessionWhenHandlesMatch() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.isRenderedFrameOwnedByCurrentSession(
            SESSION_A, SESSION_A));
    }

    @Test
    public void renderedFrameNotOwnedWhenHandlesDiffer() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.isRenderedFrameOwnedByCurrentSession(
            SESSION_A, SESSION_B));
    }

    @Test
    public void renderedFrameNotOwnedWhenCurrentSessionIsNull() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.isRenderedFrameOwnedByCurrentSession(
            null, SESSION_A));
    }

    @Test
    public void renderedFrameIsForeignWhenOwnedByDifferentSession() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.isRenderedFrameForeign(SESSION_A, SESSION_B));
    }

    @Test
    public void renderedFrameIsForeignWhenCurrentSessionIsNullButFrameHasOwner() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.isRenderedFrameForeign(null, SESSION_B));
    }

    @Test
    public void renderedFrameIsNotForeignWhenOwnedByCurrentSession() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.isRenderedFrameForeign(SESSION_A, SESSION_A));
    }

    @Test
    public void renderedFrameIsNotForeignWhenThereIsNoOwner() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.isRenderedFrameForeign(SESSION_A, null));
    }

    @Test
    public void coverIsRequiredWhenSwitchingBetweenSessionsThatShareTheSameUrl() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_B, SESSION_A, URL_X, URL_X, true));
    }

    @Test
    public void coverIsRequiredWhenTargetUrlDiffersFromLoadedUrl() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_A, SESSION_A, URL_X, URL_Y, true));
    }

    @Test
    public void coverIsNotRequiredWhenSameSessionRedisplaysTheSameUrl() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_A, SESSION_A, URL_X, URL_X, true));
    }

    @Test
    public void coverIsNeverShownWhenBrowserWillNotBeVisible() {
        Assert.assertFalse(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_B, SESSION_A, URL_X, URL_X, false));
        Assert.assertFalse(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_A, SESSION_A, URL_X, URL_Y, false));
    }

    @Test
    public void coverIsRequiredWhenForeignFrameHasNoLoadedUrlYet() {
        Assert.assertTrue(BrowserRenderedFrameOwnership.requiresCoverForFrame(
            SESSION_B, SESSION_A, null, URL_X, true));
    }
}
