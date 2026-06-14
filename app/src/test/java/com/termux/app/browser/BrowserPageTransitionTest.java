package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPageTransitionTest {

    private static final String DISPLAYED_URL = "https://previous.example/";

    private static final String TARGET_URL = "https://requested.example/";

    @Test
    public void loadingADifferentUrlWhileVisibleRequiresCoverToHideStalePage() {
        Assert.assertTrue(
            BrowserPageTransition.requiresCoverWhileLoading(DISPLAYED_URL, TARGET_URL, true));
    }

    @Test
    public void reloadingTheSameUrlWhileVisibleDoesNotRequireCover() {
        Assert.assertFalse(
            BrowserPageTransition.requiresCoverWhileLoading(TARGET_URL, TARGET_URL, true));
    }

    @Test
    public void loadingWhenNothingIsDisplayedYetRequiresCover() {
        Assert.assertTrue(
            BrowserPageTransition.requiresCoverWhileLoading(null, TARGET_URL, true));
    }

    @Test
    public void loadingADifferentUrlWhileNotVisibleDoesNotRequireCover() {
        Assert.assertFalse(
            BrowserPageTransition.requiresCoverWhileLoading(DISPLAYED_URL, TARGET_URL, false));
    }

    @Test
    public void reloadingTheSameUrlWhileNotVisibleDoesNotRequireCover() {
        Assert.assertFalse(
            BrowserPageTransition.requiresCoverWhileLoading(TARGET_URL, TARGET_URL, false));
    }
}
