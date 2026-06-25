package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserHeaderUrlMenuEligibilityTest {

    @Test
    public void allowsLoadedHttpUrl() {
        Assert.assertTrue(BrowserHeaderUrlMenuEligibility.canShowMenuFor(
            "https://github.com/HiromiShikata/termux-app/issues/907"));
    }

    @Test
    public void rejectsAboutBlank() {
        Assert.assertFalse(BrowserHeaderUrlMenuEligibility.canShowMenuFor("about:blank"));
    }

    @Test
    public void rejectsNull() {
        Assert.assertFalse(BrowserHeaderUrlMenuEligibility.canShowMenuFor(null));
    }

    @Test
    public void rejectsEmpty() {
        Assert.assertFalse(BrowserHeaderUrlMenuEligibility.canShowMenuFor(""));
    }
}
