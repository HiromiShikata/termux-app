package com.termux.app.browser;

import android.content.Intent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserInboundViewUrlTest {

    @Test
    public void httpsMeetViewUrlIsRoutedIntoInAppBrowser() {
        Assert.assertEquals("https://meet.google.com/abc-defg-hij",
            BrowserInboundViewUrl.resolveInAppBrowserUrl(
                Intent.ACTION_VIEW, "https://meet.google.com/abc-defg-hij"));
    }

    @Test
    public void httpMeetViewUrlIsRoutedIntoInAppBrowser() {
        Assert.assertEquals("http://meet.google.com/abc-defg-hij",
            BrowserInboundViewUrl.resolveInAppBrowserUrl(
                Intent.ACTION_VIEW, "http://meet.google.com/abc-defg-hij"));
    }

    @Test
    public void meetHostMatchIsCaseInsensitive() {
        Assert.assertEquals("https://Meet.Google.com/abc-defg-hij",
            BrowserInboundViewUrl.resolveInAppBrowserUrl(
                Intent.ACTION_VIEW, "https://Meet.Google.com/abc-defg-hij"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmedFromTheRoutedUrl() {
        Assert.assertEquals("https://meet.google.com/abc-defg-hij",
            BrowserInboundViewUrl.resolveInAppBrowserUrl(
                Intent.ACTION_VIEW, "  https://meet.google.com/abc-defg-hij  "));
    }

    @Test
    public void nonMeetHostIsNotRouted() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(
            Intent.ACTION_VIEW, "https://example.com/abc-defg-hij"));
    }

    @Test
    public void meetSubdomainImpersonationIsNotRouted() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(
            Intent.ACTION_VIEW, "https://meet.google.com.evil.example/abc-defg-hij"));
    }

    @Test
    public void nonViewActionIsNotRouted() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(
            Intent.ACTION_SEND, "https://meet.google.com/abc-defg-hij"));
    }

    @Test
    public void nonHttpSchemeIsNotRouted() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(
            Intent.ACTION_VIEW, "meet://meet.google.com/abc-defg-hij"));
    }

    @Test
    public void nullActionYieldsNoRouting() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(
            null, "https://meet.google.com/abc-defg-hij"));
    }

    @Test
    public void nullOrEmptyDataStringYieldsNoRouting() {
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(Intent.ACTION_VIEW, null));
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(Intent.ACTION_VIEW, ""));
        Assert.assertNull(BrowserInboundViewUrl.resolveInAppBrowserUrl(Intent.ACTION_VIEW, "   "));
    }
}
