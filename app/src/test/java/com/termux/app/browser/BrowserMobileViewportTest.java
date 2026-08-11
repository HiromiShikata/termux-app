package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserMobileViewportTest {

    @Test
    public void layoutContentRequestsDeviceWidth() {
        Assert.assertTrue(BrowserMobileViewport.LAYOUT_CONTENT.contains("width=device-width"));
    }

    @Test
    public void injectionScriptForcesDeviceWidthViewport() {
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT
            .contains(BrowserMobileViewport.LAYOUT_CONTENT));
    }

    @Test
    public void injectionScriptTargetsViewportMeta() {
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT.contains("meta[name=\"viewport\"]"));
    }

    @Test
    public void injectionScriptCreatesViewportMetaWhenMissing() {
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT.contains("createElement('meta')"));
    }

    @Test
    public void injectionScriptReappliesOnSiteViewportChanges() {
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT.contains("MutationObserver"));
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT.contains(".observe(document.documentElement"));
    }

    @Test
    public void injectionScriptReturnsEarlyWhenAlreadyInstalledForTheDocument() {
        Assert.assertTrue(BrowserMobileViewport.INJECTION_SCRIPT
            .contains("if(Object.getOwnPropertyDescriptor(window,observerName)){return;}"));
    }
}
