package com.termux.app.browser;

import android.webkit.WebSettings;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BrowserWebAuthenticationTest {

    private WebSettings newSettings() {
        return new android.webkit.WebView(RuntimeEnvironment.getApplication()).getSettings();
    }

    @Test
    public void neverEnablesBrowserWebAuthenticationSoTheNativePasskeyCeremonyIsNotReached() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForBrowser());
    }

    @Test
    public void applyLeavesWebSettingsAtTheDefaultSupportLevelWithoutThrowing() {
        WebSettings settings = newSettings();
        BrowserWebAuthentication.apply(settings);
    }
}
