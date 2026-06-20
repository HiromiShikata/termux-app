package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionPickerScrollOffsetTest {

    @Test
    public void doesNotScrollWhenAllContentFitsInViewport() {
        int target = SessionPickerScrollOffset.targetScrollY(80, 100, 320, 200, 0);
        Assert.assertEquals(0, target);
    }

    @Test
    public void keepsScrollUnchangedWhenHighlightedLineAlreadyFullyVisible() {
        int target = SessionPickerScrollOffset.targetScrollY(150, 170, 100, 600, 120);
        Assert.assertEquals(120, target);
    }

    @Test
    public void scrollsDownToRevealHighlightedLineNearTheBottom() {
        int contentHeight = 1000;
        int viewportHeight = 100;
        int highlightedLineTop = 940;
        int highlightedLineBottom = 960;
        int target = SessionPickerScrollOffset.targetScrollY(
            highlightedLineTop, highlightedLineBottom, viewportHeight, contentHeight, 0);
        Assert.assertTrue(highlightedLineTop >= target);
        Assert.assertTrue(highlightedLineBottom <= target + viewportHeight);
    }

    @Test
    public void clampsToMaximumScrollForTheVeryLastLine() {
        int contentHeight = 1000;
        int viewportHeight = 100;
        int maxScrollY = contentHeight - viewportHeight;
        int target = SessionPickerScrollOffset.targetScrollY(
            980, 1000, viewportHeight, contentHeight, 0);
        Assert.assertEquals(maxScrollY, target);
    }

    @Test
    public void scrollsUpToRevealHighlightedLineAboveTheViewport() {
        int contentHeight = 1000;
        int viewportHeight = 100;
        int highlightedLineTop = 200;
        int highlightedLineBottom = 220;
        int currentScrollY = 500;
        int target = SessionPickerScrollOffset.targetScrollY(
            highlightedLineTop, highlightedLineBottom, viewportHeight, contentHeight, currentScrollY);
        Assert.assertTrue(highlightedLineTop >= target);
        Assert.assertTrue(highlightedLineBottom <= target + viewportHeight);
        Assert.assertTrue(target < currentScrollY);
    }

    @Test
    public void centersHighlightedLineWhenScrollingToReachIt() {
        int contentHeight = 2000;
        int viewportHeight = 100;
        int highlightedLineTop = 1000;
        int highlightedLineBottom = 1020;
        int target = SessionPickerScrollOffset.targetScrollY(
            highlightedLineTop, highlightedLineBottom, viewportHeight, contentHeight, 0);
        int expectedCentered = highlightedLineTop - (viewportHeight - (highlightedLineBottom - highlightedLineTop)) / 2;
        Assert.assertEquals(expectedCentered, target);
    }

    @Test
    public void neverReturnsNegativeScroll() {
        int target = SessionPickerScrollOffset.targetScrollY(0, 20, 100, 1000, 0);
        Assert.assertEquals(0, target);
    }

    @Test
    public void alignsToLineTopWhenHighlightedLineTallerThanViewport() {
        int contentHeight = 1000;
        int viewportHeight = 50;
        int highlightedLineTop = 400;
        int highlightedLineBottom = 480;
        int target = SessionPickerScrollOffset.targetScrollY(
            highlightedLineTop, highlightedLineBottom, viewportHeight, contentHeight, 0);
        Assert.assertEquals(highlightedLineTop, target);
    }
}
