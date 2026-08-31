package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserBackPressDecisionTest {

    @Test
    public void returnsNotHandledWhenBrowserIsNotVisible() {
        Assert.assertEquals(BrowserBackPressDecision.Result.NOT_HANDLED,
            BrowserBackPressDecision.resolve(false, true, true));
    }

    @Test
    public void returnsShowTerminalWhenBrowserVisibleButNoWebView() {
        Assert.assertEquals(BrowserBackPressDecision.Result.SHOW_TERMINAL,
            BrowserBackPressDecision.resolve(true, false, false));
    }

    @Test
    public void returnsNavigateBackWhenBrowserVisibleWithWebViewAndHistory() {
        Assert.assertEquals(BrowserBackPressDecision.Result.NAVIGATE_BACK,
            BrowserBackPressDecision.resolve(true, true, true));
    }

    @Test
    public void returnsKeepBrowserOpenWhenBrowserVisibleWithWebViewButNoHistory() {
        Assert.assertEquals(BrowserBackPressDecision.Result.KEEP_BROWSER_OPEN,
            BrowserBackPressDecision.resolve(true, true, false));
    }
}
