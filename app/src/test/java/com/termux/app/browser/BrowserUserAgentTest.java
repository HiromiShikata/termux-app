package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserUserAgentTest {

    private static final String ENGINE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6 Build/TQ3A.230805.001; wv) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/123.0.6312.80 Mobile Safari/537.36";

    @Test
    public void theEngineMajorVersionIsReadOutOfTheEngineOwnUserAgent() {
        Assert.assertEquals("123", BrowserUserAgent.engineMajorVersion(ENGINE_USER_AGENT));
    }

    @Test
    public void theEngineMajorVersionIsAbsentWhenTheUserAgentNamesNoEngineVersion() {
        Assert.assertNull(BrowserUserAgent.engineMajorVersion("Mozilla/5.0 (Linux; Android 13; Pixel 6)"));
        Assert.assertNull(BrowserUserAgent.engineMajorVersion(null));
    }

    @Test
    public void theDesktopUserAgentCarriesTheVersionTheEngineItselfReports() {
        String desktopUserAgent = BrowserUserAgent.desktopUserAgentFrom(ENGINE_USER_AGENT);

        Assert.assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/123.0.0.0 Safari/537.36",
            desktopUserAgent);
    }

    @Test
    public void theDesktopUserAgentDropsTheMobileAndWebViewTokensTheEngineSends() {
        String desktopUserAgent = BrowserUserAgent.desktopUserAgentFrom(ENGINE_USER_AGENT);

        Assert.assertFalse("requesting the desktop layout means asking for the desktop platform token: "
            + desktopUserAgent, desktopUserAgent.contains("Mobile"));
        Assert.assertFalse("requesting the desktop layout means asking for the desktop platform token: "
            + desktopUserAgent, desktopUserAgent.contains("Android"));
        Assert.assertFalse(desktopUserAgent.contains("wv"));
    }

    @Test
    public void thereIsNoDesktopUserAgentToDeriveWhenTheEngineNamesNoVersion() {
        Assert.assertNull("a version that cannot be read must not be replaced with a made-up one, because a"
                + " version that disagrees with the engine is exactly what this class exists to stop sending",
            BrowserUserAgent.desktopUserAgentFrom("Mozilla/5.0 (Linux; Android 13; Pixel 6)"));
        Assert.assertNull(BrowserUserAgent.desktopUserAgentFrom(null));
    }
}
