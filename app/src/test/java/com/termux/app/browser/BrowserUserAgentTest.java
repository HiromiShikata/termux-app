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
    public void desktopUserAgentIsGenuineDesktopChrome() {
        Assert.assertTrue(BrowserUserAgent.DESKTOP_USER_AGENT.contains("X11; Linux x86_64"));
        Assert.assertTrue(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Chrome/"));
        Assert.assertTrue(BrowserUserAgent.DESKTOP_USER_AGENT.contains("Safari/537.36"));
    }
}
