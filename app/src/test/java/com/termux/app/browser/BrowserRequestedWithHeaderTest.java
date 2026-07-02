package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserRequestedWithHeaderTest {

    @Test
    public void suppressesHeaderWhenWebViewProvidesOriginAllowList() {
        Assert.assertTrue(BrowserRequestedWithHeader.shouldSuppressForBrowser(true));
    }

    @Test
    public void leavesHeaderUnmanagedWhenWebViewLacksOriginAllowList() {
        Assert.assertFalse(BrowserRequestedWithHeader.shouldSuppressForBrowser(false));
    }

    @Test
    public void requiresTheRequestedWithHeaderAllowListWebViewFeature() {
        Assert.assertEquals("REQUESTED_WITH_HEADER_ALLOW_LIST", BrowserRequestedWithHeader.REQUIRED_FEATURE);
    }

    @Test
    public void usesEmptyAllowListSoThePackageNameReachesNoOrigin() {
        Assert.assertTrue(BrowserRequestedWithHeader.SUPPRESSED_ORIGIN_ALLOW_LIST.isEmpty());
    }

    @Test
    public void emptyAllowListExcludesGoogleAuthOrigins() {
        Assert.assertFalse(
            BrowserRequestedWithHeader.SUPPRESSED_ORIGIN_ALLOW_LIST.contains("https://accounts.google.com"));
        Assert.assertFalse(
            BrowserRequestedWithHeader.SUPPRESSED_ORIGIN_ALLOW_LIST.contains("https://google.com"));
    }
}
