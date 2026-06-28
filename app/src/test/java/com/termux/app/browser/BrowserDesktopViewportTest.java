package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserDesktopViewportTest {

    @Test
    public void layoutWidthIsFullDesktopBreakpoint() {
        Assert.assertEquals(1280, BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX);
    }

    @Test
    public void injectionScriptForcesDesktopLayoutWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT
            .contains("width=" + BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX));
    }

    @Test
    public void injectionScriptForcesFullDesktopBreakpointWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("width=1280"));
    }

    @Test
    public void injectionScriptTargetsViewportMeta() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("meta[name=\"viewport\"]"));
    }

    @Test
    public void injectionScriptCreatesViewportMetaWhenMissing() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("createElement('meta')"));
    }

    @Test
    public void injectionScriptReappliesOnSiteViewportChanges() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("MutationObserver"));
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains(".observe(document.documentElement"));
    }

    @Test
    public void injectionScriptForcesEveryViewportMeta() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT
            .contains("querySelectorAll('meta[name=\"viewport\"]')"));
    }

    @Test
    public void injectionScriptSkipsRewriteWhenAlreadyDesktopWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT
            .contains("getAttribute('content')!==desktopContent"));
    }

    @Test
    public void injectionScriptReturnsEarlyWhenAlreadyInstalledForTheDocument() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT
            .contains("if(window.__termuxDesktopViewportObserver){return;}"));
    }

    @Test
    public void appliesToDesktopModeTab() {
        BrowserTab tab = new BrowserTab("session", "https://github.com/");
        Assert.assertTrue(tab.isDesktopMode());
        Assert.assertTrue(BrowserDesktopViewport.appliesTo(tab));
    }

    @Test
    public void doesNotApplyToMobileModeTab() {
        BrowserTab tab = new BrowserTab("session", "https://github.com/");
        tab.setDesktopMode(false);
        Assert.assertFalse(BrowserDesktopViewport.appliesTo(tab));
    }

    @Test
    public void doesNotApplyToNullTab() {
        Assert.assertFalse(BrowserDesktopViewport.appliesTo(null));
    }
}
