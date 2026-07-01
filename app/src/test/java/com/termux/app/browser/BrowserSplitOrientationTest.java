package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSplitOrientationTest {

    private static final int ANDROID_ORIENTATION_UNDEFINED = 0;

    private static final int ANDROID_ORIENTATION_PORTRAIT = 1;

    private static final int ANDROID_ORIENTATION_LANDSCAPE = 2;

    @Test
    public void resolvesLandscapeWhenConfigurationOrientationMatchesLandscape() {
        Assert.assertEquals(
            BrowserSplitOrientation.LANDSCAPE,
            BrowserSplitOrientation.resolve(ANDROID_ORIENTATION_LANDSCAPE, ANDROID_ORIENTATION_LANDSCAPE));
    }

    @Test
    public void resolvesPortraitWhenConfigurationOrientationIsPortrait() {
        Assert.assertEquals(
            BrowserSplitOrientation.PORTRAIT,
            BrowserSplitOrientation.resolve(ANDROID_ORIENTATION_PORTRAIT, ANDROID_ORIENTATION_LANDSCAPE));
    }

    @Test
    public void resolvesPortraitWhenConfigurationOrientationIsUndefined() {
        Assert.assertEquals(
            BrowserSplitOrientation.PORTRAIT,
            BrowserSplitOrientation.resolve(ANDROID_ORIENTATION_UNDEFINED, ANDROID_ORIENTATION_LANDSCAPE));
    }

    @Test
    public void landscapeAppliesRatioToWidth() {
        Assert.assertTrue(BrowserSplitOrientation.LANDSCAPE.ratioAppliesToWidth());
    }

    @Test
    public void portraitAppliesRatioToHeightNotWidth() {
        Assert.assertFalse(BrowserSplitOrientation.PORTRAIT.ratioAppliesToWidth());
    }

    @Test
    public void landscapeDividerTracksHorizontalAxis() {
        Assert.assertTrue(BrowserSplitOrientation.LANDSCAPE.dividerTracksHorizontalAxis());
    }

    @Test
    public void portraitDividerTracksVerticalAxisNotHorizontal() {
        Assert.assertFalse(BrowserSplitOrientation.PORTRAIT.dividerTracksHorizontalAxis());
    }

    @Test
    public void reportsLandscapeOnlyForTheLandscapeConstant() {
        Assert.assertTrue(BrowserSplitOrientation.LANDSCAPE.isLandscape());
        Assert.assertFalse(BrowserSplitOrientation.PORTRAIT.isLandscape());
    }
}
