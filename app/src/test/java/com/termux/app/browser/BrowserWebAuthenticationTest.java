package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserWebAuthenticationTest {

    @Test
    public void enablesPasskeySupportWhenWebViewProvidesWebAuthentication() {
        Assert.assertTrue(BrowserWebAuthentication.shouldEnableForApp(true));
    }

    @Test
    public void leavesPasskeySupportDisabledWhenWebViewLacksWebAuthentication() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForApp(false));
    }

    @Test
    public void requiresTheWebAuthenticationWebViewFeature() {
        Assert.assertEquals("WEB_AUTHENTICATION", BrowserWebAuthentication.REQUIRED_FEATURE);
    }
}
