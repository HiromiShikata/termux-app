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

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    @Test
    public void mobileModeAppliesCleanMobileUserAgentWithoutWebViewMarker() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertEquals(BrowserUserAgent.MOBILE_USER_AGENT, settings.getUserAgentString());
        Assert.assertTrue(settings.getUserAgentString().contains("Chrome/"));
        Assert.assertFalse(settings.getUserAgentString().contains("wv"));
    }

    @Test
    public void mobileModeEnablesJavaScriptAndDomStorageForLogin() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertTrue(settings.getJavaScriptEnabled());
        Assert.assertTrue(settings.getDomStorageEnabled());
    }

    @Test
    public void desktopModeUsesDesktopUserAgentRegardlessOfDefault() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        Assert.assertEquals(BrowserUserAgent.DESKTOP_USER_AGENT, webView.getSettings().getUserAgentString());
    }

    @Test
    public void desktopUserAgentDoesNotAdvertiseMobile() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertFalse(settings.getUserAgentString().contains("Mobile"));
        Assert.assertFalse(settings.getUserAgentString().contains("Android"));
    }

    @Test
    public void commonSettingsAreEnabledForMobileMode() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        assertCommonSettings(webView.getSettings());
    }

    @Test
    public void commonSettingsAreEnabledForDesktopMode() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        assertCommonSettings(webView.getSettings());
    }

    @Test
    public void mobileModeDisablesOverviewModeSoPagesAreNotZoomedOutToFitWideContent() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        Assert.assertFalse(webView.getSettings().getLoadWithOverviewMode());
    }

    @Test
    public void desktopModeEnablesOverviewModeToFitWideDesktopLayout() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        Assert.assertTrue(webView.getSettings().getLoadWithOverviewMode());
    }

    @Test
    public void mobileAndDesktopShareIdenticalCommonSettingsExceptOverviewMode() {
        WebView mobileWebView = newWebView();
        WebView desktopWebView = newWebView();
        BrowserWebViewConfigurator.apply(mobileWebView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        BrowserWebViewConfigurator.apply(desktopWebView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        WebSettings mobile = mobileWebView.getSettings();
        WebSettings desktop = desktopWebView.getSettings();

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
            mobile.getAllowFileAccess(), desktop.getAllowFileAccess());
        Assert.assertEquals(
            mobile.getAllowContentAccess(), desktop.getAllowContentAccess());
        Assert.assertNotEquals(
            mobile.getLoadWithOverviewMode(), desktop.getLoadWithOverviewMode());
    }

    @Test
    public void multipleWindowsSupportIsEnabledSoNewTabLinksReachOnCreateWindow() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, DEFAULT_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertTrue(settings.supportMultipleWindows());
        Assert.assertTrue(settings.getJavaScriptCanOpenWindowsAutomatically());
    }

    @Test
    public void requestedWithHeaderSuppressionIsWiredWithoutBreakingOtherSettings() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, DEFAULT_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertEquals(BrowserUserAgent.MOBILE_USER_AGENT, settings.getUserAgentString());
        assertCommonSettings(settings);
    }

    @Test
    public void mobileModeWithNullDefaultUserAgentStillAppliesCleanMobileUserAgent() {
        WebView webView = newWebView();
        webView.getSettings().setUserAgentString(null);
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, null);
        WebSettings settings = webView.getSettings();
        Assert.assertEquals(BrowserUserAgent.MOBILE_USER_AGENT, settings.getUserAgentString());
        Assert.assertFalse(settings.getUserAgentString().contains("wv"));
    }

    private void assertCommonSettings(WebSettings settings) {
        Assert.assertTrue(settings.getJavaScriptEnabled());
        Assert.assertTrue(settings.getDomStorageEnabled());
        Assert.assertTrue(settings.getBuiltInZoomControls());
        Assert.assertFalse(settings.getDisplayZoomControls());
        Assert.assertTrue(settings.getUseWideViewPort());
        Assert.assertFalse(settings.getAllowFileAccess());
        Assert.assertFalse(settings.getAllowContentAccess());
    }
}
