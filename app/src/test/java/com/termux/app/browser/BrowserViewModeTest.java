package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserViewModeTest {

    @Test
    public void desktopModeReportsDesktop() {
        Assert.assertTrue(BrowserViewMode.DESKTOP.isDesktop());
        Assert.assertFalse(BrowserViewMode.DESKTOP.isMobile());
    }

    @Test
    public void mobileModeReportsMobile() {
        Assert.assertTrue(BrowserViewMode.MOBILE.isMobile());
        Assert.assertFalse(BrowserViewMode.MOBILE.isDesktop());
    }
}
