package com.termux.app.browser;

import android.webkit.WebSettings;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BrowserWebViewConfiguratorTest {

    private static final String DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel) AppleWebKit/537.36 Mobile Safari/537.36";

    private WebSettings newSettings() {
        return new WebView(RuntimeEnvironment.getApplication()).getSettings();
    }

    @Test
    public void mobileModeUsesProvidedDefaultUserAgent() {
        WebSettings settings = newSettings();
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        Assert.assertEquals(DEFAULT_USER_AGENT, settings.getUserAgentString());
    }

    @Test
    public void desktopModeUsesDesktopUserAgentRegardlessOfDefault() {
        WebSettings settings = newSettings();
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        Assert.assertEquals(BrowserUserAgent.DESKTOP_USER_AGENT, settings.getUserAgentString());
    }

    @Test
    public void desktopUserAgentDoesNotAdvertiseMobile() {
        WebSettings settings = newSettings();
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        Assert.assertFalse(settings.getUserAgentString().contains("Mobile"));
        Assert.assertFalse(settings.getUserAgentString().contains("Android"));
    }

    @Test
    public void commonSettingsAreEnabledForMobileMode() {
        WebSettings settings = newSettings();
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        assertCommonSettings(settings);
    }

    @Test
    public void commonSettingsAreEnabledForDesktopMode() {
        WebSettings settings = newSettings();
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        assertCommonSettings(settings);
    }

    @Test
    public void mobileAndDesktopShareIdenticalCommonSettings() {
        WebSettings mobile = newSettings();
        WebSettings desktop = newSettings();
        BrowserWebViewConfigurator.apply(mobile, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        BrowserWebViewConfigurator.apply(desktop, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);

        Assert.assertEquals(
            mobile.getJavaScriptEnabled(), desktop.getJavaScriptEnabled());
        Assert.assertEquals(
            mobile.getDomStorageEnabled(), desktop.getDomStorageEnabled());
        Assert.assertEquals(
            mobile.getBuiltInZoomControls(), desktop.getBuiltInZoomControls());
        Assert.assertEquals(
            mobile.getDisplayZoomControls(), desktop.getDisplayZoomControls());
        Assert.assertEquals(
            mobile.getUseWideViewPort(), desktop.getUseWideViewPort());
        Assert.assertEquals(
            mobile.getLoadWithOverviewMode(), desktop.getLoadWithOverviewMode());
        Assert.assertEquals(
            mobile.getAllowFileAccess(), desktop.getAllowFileAccess());
        Assert.assertEquals(
            mobile.getAllowContentAccess(), desktop.getAllowContentAccess());
    }

    @Test
    public void mobileModeWithNullDefaultUserAgentLeavesUserAgentNull() {
        WebSettings settings = newSettings();
        settings.setUserAgentString(null);
        BrowserWebViewConfigurator.apply(settings, BrowserViewMode.MOBILE, null);
        Assert.assertNotEquals(BrowserUserAgent.DESKTOP_USER_AGENT, settings.getUserAgentString());
    }

    private void assertCommonSettings(WebSettings settings) {
        Assert.assertTrue(settings.getJavaScriptEnabled());
        Assert.assertTrue(settings.getDomStorageEnabled());
        Assert.assertTrue(settings.getBuiltInZoomControls());
        Assert.assertFalse(settings.getDisplayZoomControls());
        Assert.assertTrue(settings.getUseWideViewPort());
        Assert.assertTrue(settings.getLoadWithOverviewMode());
        Assert.assertFalse(settings.getAllowFileAccess());
        Assert.assertFalse(settings.getAllowContentAccess());
    }
}
