package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserRenderProcessGoneDecisionTest {

    @Test
    public void recreatesAndNotifiesWhenDisplayedTabRendererCrashed() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(true, true, true);

        Assert.assertTrue(decision.shouldRecreateWebView());
        Assert.assertTrue(decision.shouldReloadCrashedUrl());
        Assert.assertFalse(decision.shouldLoadBlankPageInsteadOfReloading());
        Assert.assertTrue(decision.shouldNotifyUser());
        Assert.assertTrue(decision.didCrash());
    }

    @Test
    public void stopsReloadingTheCrashedUrlButStillRecreatesWhenRendererIsLooping() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(true, true, true, true);

        Assert.assertTrue(decision.shouldRecreateWebView());
        Assert.assertFalse(decision.shouldReloadCrashedUrl());
        Assert.assertTrue(decision.shouldLoadBlankPageInsteadOfReloading());
        Assert.assertTrue(decision.shouldNotifyUser());
    }

    @Test
    public void doesNotLoadBlankPageWhenLoopingTabIsUnknown() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(false, false, true, true);

        Assert.assertFalse(decision.shouldRecreateWebView());
        Assert.assertFalse(decision.shouldLoadBlankPageInsteadOfReloading());
        Assert.assertFalse(decision.shouldReloadCrashedUrl());
    }

    @Test
    public void recreatesButDoesNotNotifyWhenBackgroundTabRendererDied() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(true, false, true);

        Assert.assertTrue(decision.shouldRecreateWebView());
        Assert.assertFalse(decision.shouldNotifyUser());
    }

    @Test
    public void recreatesWhenSystemKilledRendererWithoutCrash() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(true, true, false);

        Assert.assertTrue(decision.shouldRecreateWebView());
        Assert.assertTrue(decision.shouldNotifyUser());
        Assert.assertFalse(decision.didCrash());
    }

    @Test
    public void doesNotRecreateWhenDeadWebViewHasNoKnownTab() {
        BrowserRenderProcessGoneDecision decision =
            BrowserRenderProcessGoneDecision.forDiedWebView(false, false, true);

        Assert.assertFalse(decision.shouldRecreateWebView());
        Assert.assertFalse(decision.shouldNotifyUser());
    }
}
