package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabStateRestorationTest {

    @Test
    public void aTabWithoutSavedStateMustLoadItsUrl() {
        BrowserTabStateRestoration restoration =
            BrowserTabStateRestoration.resolve(false, false);

        Assert.assertEquals(BrowserTabStateRestoration.Action.LOAD, restoration.getAction());
        Assert.assertTrue(restoration.shouldLoadUrl());
        Assert.assertFalse(restoration.shouldRestoreState());
    }

    @Test
    public void aTabWithSavedStateMustRestoreItsOwnHistory() {
        BrowserTabStateRestoration restoration =
            BrowserTabStateRestoration.resolve(true, false);

        Assert.assertEquals(BrowserTabStateRestoration.Action.RESTORE, restoration.getAction());
        Assert.assertTrue(restoration.shouldRestoreState());
        Assert.assertFalse(restoration.shouldLoadUrl());
    }

    @Test
    public void aForcedReloadAlwaysLoadsAFreshUrlEvenWhenStateExists() {
        BrowserTabStateRestoration restoration =
            BrowserTabStateRestoration.resolve(true, true);

        Assert.assertEquals(BrowserTabStateRestoration.Action.LOAD, restoration.getAction());
        Assert.assertTrue(restoration.shouldLoadUrl());
        Assert.assertFalse(restoration.shouldRestoreState());
    }

    @Test
    public void aForcedReloadWithoutSavedStateStillLoadsTheUrl() {
        BrowserTabStateRestoration restoration =
            BrowserTabStateRestoration.resolve(false, true);

        Assert.assertEquals(BrowserTabStateRestoration.Action.LOAD, restoration.getAction());
        Assert.assertTrue(restoration.shouldLoadUrl());
    }
}
