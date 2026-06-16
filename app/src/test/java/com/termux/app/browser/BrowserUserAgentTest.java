package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserUserAgentTest {

    private static final String DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel) AppleWebKit/537.36 Mobile Safari/537.36";

    @Test
    public void resolvesDesktopUserAgentWhenDesktopModeEnabled() {
        Assert.assertEquals(BrowserUserAgent.DESKTOP_USER_AGENT,
            BrowserUserAgent.resolve(true, DEFAULT_USER_AGENT));
    }

    @Test
    public void resolvesDefaultUserAgentWhenDesktopModeDisabled() {
        Assert.assertEquals(DEFAULT_USER_AGENT,
            BrowserUserAgent.resolve(false, DEFAULT_USER_AGENT));
    }

    @Test
    public void resolvesNullDefaultUserAgentWhenDesktopModeDisabled() {
        Assert.assertNull(BrowserUserAgent.resolve(false, null));
    }

    @Test
    public void desktopUserAgentDoesNotAdvertiseMobile() {
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Mobile"));
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Android"));
        Assert.assertFalse(BrowserUserAgent.DESKTOP_USER_AGENT.contains("wv"));
    }

    @Test
    public void normalizeDefaultRemovesSemicolonWebViewMarker() {
        String webViewUserAgent =
            "Mozilla/5.0 (Linux; Android 13; Pixel; wv) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36";
        String normalized = BrowserUserAgent.normalizeDefault(webViewUserAgent);
        Assert.assertEquals(
            "Mozilla/5.0 (Linux; Android 13; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36",
            normalized);
        Assert.assertFalse(normalized.contains("wv"));
    }

    @Test
    public void normalizeDefaultRemovesStandaloneWebViewMarker() {
        Assert.assertEquals(
            "Mozilla/5.0 (Linux; Android 13; Pixel) AppleWebKit/537.36",
            BrowserUserAgent.normalizeDefault(
                "Mozilla/5.0 (Linux; Android 13; Pixel wv) AppleWebKit/537.36"));
    }

    @Test
    public void normalizeDefaultReturnsNullForNullInput() {
        Assert.assertNull(BrowserUserAgent.normalizeDefault(null));
    }

    @Test
    public void normalizeDefaultLeavesUserAgentWithoutMarkerUnchanged() {
        Assert.assertEquals(DEFAULT_USER_AGENT, BrowserUserAgent.normalizeDefault(DEFAULT_USER_AGENT));
    }

    @Test
    public void normalizeDefaultDoesNotRemoveSubstringsContainingMarkerLetters() {
        String userAgent =
            "Mozilla/5.0 (Linux; Android 13; WebViewer Pixel) AppleWebKit/537.36 review Safari/537.36";
        Assert.assertEquals(userAgent, BrowserUserAgent.normalizeDefault(userAgent));
    }

    @Test
    public void resolveReturnsNormalizedDefaultWhenDesktopModeDisabled() {
        String mDefaultUserAgent = BrowserUserAgent.normalizeDefault(
            "Mozilla/5.0 (Linux; Android 13; Pixel; wv) AppleWebKit/537.36 Mobile Safari/537.36");
        Assert.assertEquals(mDefaultUserAgent, BrowserUserAgent.resolve(false, mDefaultUserAgent));
        Assert.assertFalse(BrowserUserAgent.resolve(false, mDefaultUserAgent).contains("wv"));
    }
}
