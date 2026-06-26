package com.termux.app.browser;

import androidx.webkit.WebSettingsCompat;

import org.junit.Assert;
import org.junit.Test;

public class BrowserWebAuthenticationTest {

    @Test
    public void enablesPasskeySupportWhenWebViewProvidesWebAuthentication() {
        Assert.assertTrue(BrowserWebAuthentication.shouldEnableForBrowser(true));
    }

    @Test
    public void leavesPasskeySupportDisabledWhenWebViewLacksWebAuthentication() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForBrowser(false));
    }

    @Test
    public void requiresTheWebAuthenticationWebViewFeature() {
        Assert.assertEquals("WEB_AUTHENTICATION", BrowserWebAuthentication.REQUIRED_FEATURE);
    }

    @Test
    public void usesBrowserSupportLevelSoArbitrarySiteOriginsAreAllowed() {
        Assert.assertEquals(
            WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER,
            BrowserWebAuthentication.SUPPORT_LEVEL);
    }
}
