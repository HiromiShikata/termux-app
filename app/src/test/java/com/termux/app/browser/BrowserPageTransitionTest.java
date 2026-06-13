package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPageTransitionTest {

    private static final String SESSION = "session-a";

    @Test
    public void openingNewTabFromContextMenuRequiresBlankToHideStalePage() {
        BrowserTab previouslyDisplayedTab = new BrowserTab(SESSION, "https://previous.example/");
        BrowserTab newlyRequestedTab = new BrowserTab(SESSION, "https://requested.example/");

        Assert.assertTrue(
            BrowserPageTransition.requiresBlankBeforeLoad(previouslyDisplayedTab, newlyRequestedTab));
    }

    @Test
    public void switchingBackToAlreadyDisplayedTabDoesNotRequireBlank() {
        BrowserTab displayedTab = new BrowserTab(SESSION, "https://open.example/");

        Assert.assertFalse(
            BrowserPageTransition.requiresBlankBeforeLoad(displayedTab, displayedTab));
    }

    @Test
    public void openingTabWhenNothingDisplayedRequiresBlank() {
        BrowserTab newlyRequestedTab = new BrowserTab(SESSION, "https://requested.example/");

        Assert.assertTrue(
            BrowserPageTransition.requiresBlankBeforeLoad(null, newlyRequestedTab));
    }

    @Test
    public void switchingToADifferentExistingTabRequiresBlank() {
        BrowserTab firstTab = new BrowserTab(SESSION, "https://first.example/");
        BrowserTab secondTab = new BrowserTab(SESSION, "https://second.example/");

        Assert.assertTrue(
            BrowserPageTransition.requiresBlankBeforeLoad(firstTab, secondTab));
    }
}
