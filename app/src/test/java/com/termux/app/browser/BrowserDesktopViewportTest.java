package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserDesktopViewportTest {

    @Test
    public void layoutWidthIsDesktopSized() {
        Assert.assertTrue(BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX >= 1024);
    }

    @Test
    public void injectionScriptForcesDesktopLayoutWidth() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT
            .contains("width=" + BrowserDesktopViewport.LAYOUT_WIDTH_CSS_PX));
    }

    @Test
    public void injectionScriptTargetsViewportMeta() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("meta[name=\"viewport\"]"));
    }

    @Test
    public void injectionScriptCreatesViewportMetaWhenMissing() {
        Assert.assertTrue(BrowserDesktopViewport.INJECTION_SCRIPT.contains("createElement('meta')"));
    }
}
