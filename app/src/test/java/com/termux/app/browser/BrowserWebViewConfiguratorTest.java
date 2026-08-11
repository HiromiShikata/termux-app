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

    private static final String ENGINE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6 Build/TQ3A.230805.001; wv) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/123.0.6312.80 Mobile Safari/537.36";

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    @Test
    public void mobileModeLeavesTheUserAgentTheEngineItselfSends() {
        WebView webView = newWebView();
        WebSettings settings = webView.getSettings();
        String userAgentBeforeConfiguration = settings.getUserAgentString();

        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);

        Assert.assertEquals(
            "a mobile tab must send the engine's own user agent, because any substituted string disagrees"
                + " with the Client Hints the same engine sends and marks the request as a rewritten client",
            userAgentBeforeConfiguration, settings.getUserAgentString());
    }

    @Test
    public void mobileModeEnablesJavaScriptAndDomStorageForLogin() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertTrue(settings.getJavaScriptEnabled());
        Assert.assertTrue(settings.getDomStorageEnabled());
    }

    @Test
    public void desktopModeAdvertisesTheVersionTheEngineReports() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        Assert.assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/123.0.0.0 Safari/537.36",
            webView.getSettings().getUserAgentString());
    }

    @Test
    public void desktopUserAgentDoesNotAdvertiseMobile() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertFalse(settings.getUserAgentString().contains("Mobile"));
        Assert.assertFalse(settings.getUserAgentString().contains("Android"));
    }

    @Test
    public void commonSettingsAreEnabledForMobileMode() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);
        assertCommonSettings(webView.getSettings());
    }

    @Test
    public void commonSettingsAreEnabledForDesktopMode() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        assertCommonSettings(webView.getSettings());
    }

    @Test
    public void mobileModeDisablesOverviewModeSoPagesAreNotZoomedOutToFitWideContent() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);
        Assert.assertFalse(webView.getSettings().getLoadWithOverviewMode());
    }

    @Test
    public void desktopModeEnablesOverviewModeToFitWideDesktopLayout() {
        WebView webView = newWebView();
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        Assert.assertTrue(webView.getSettings().getLoadWithOverviewMode());
    }

    @Test
    public void mobileAndDesktopShareIdenticalCommonSettingsExceptOverviewMode() {
        WebView mobileWebView = newWebView();
        WebView desktopWebView = newWebView();
        BrowserWebViewConfigurator.apply(mobileWebView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);
        BrowserWebViewConfigurator.apply(desktopWebView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
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
        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, ENGINE_USER_AGENT);
        WebSettings settings = webView.getSettings();
        Assert.assertTrue(settings.supportMultipleWindows());
        Assert.assertTrue(settings.getJavaScriptCanOpenWindowsAutomatically());
    }

    @Test
    public void requestedWithHeaderSuppressionIsWiredWithoutBreakingOtherSettings() {
        WebView webView = newWebView();
        WebSettings settings = webView.getSettings();
        String userAgentBeforeConfiguration = settings.getUserAgentString();

        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.MOBILE, ENGINE_USER_AGENT);

        Assert.assertEquals(userAgentBeforeConfiguration, settings.getUserAgentString());
        assertCommonSettings(settings);
    }

    @Test
    public void desktopModeKeepsTheEngineUserAgentWhenNoEngineVersionCanBeRead() {
        WebView webView = newWebView();
        WebSettings settings = webView.getSettings();
        String userAgentBeforeConfiguration = settings.getUserAgentString();

        BrowserWebViewConfigurator.apply(webView, BrowserViewMode.DESKTOP, null);

        Assert.assertEquals(
            "with no engine version to carry over, the engine's own user agent is the only one that stays"
                + " consistent with the rest of the request, so it must be left untouched",
            userAgentBeforeConfiguration, settings.getUserAgentString());
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
