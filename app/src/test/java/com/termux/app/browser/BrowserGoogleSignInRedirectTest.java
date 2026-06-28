package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserGoogleSignInRedirectTest {

    @Test
    public void accountsGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.com/signin/v2/identifier"));
    }

    @Test
    public void docsGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://docs.google.com/spreadsheets/d/abc123/edit"));
    }

    @Test
    public void sheetsGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://sheets.google.com/"));
    }

    @Test
    public void driveGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://drive.google.com/drive/my-drive"));
    }

    @Test
    public void slidesGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://slides.google.com/"));
    }

    @Test
    public void formsGoogleHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://forms.google.com/"));
    }

    @Test
    public void hostMatchIsCaseInsensitive() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://ACCOUNTS.GOOGLE.COM/signin"));
    }

    @Test
    public void nonGoogleHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://example.com/page"));
    }

    @Test
    public void googleSearchHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/search?q=test"));
    }

    @Test
    public void mailGoogleHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://mail.google.com/"));
    }

    @Test
    public void lookalikeSuffixHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.com.evil.example/signin"));
    }

    @Test
    public void nullUrlStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(null));
    }

    @Test
    public void emptyUrlStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser("   "));
    }

    @Test
    public void nonHttpSchemeStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "intent://accounts.google.com/signin"));
    }

    @Test
    public void externalBrowserHostsListIsExposed() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.externalBrowserHosts()
            .contains("accounts.google.com"));
        Assert.assertEquals(6, BrowserGoogleSignInRedirect.externalBrowserHosts().size());
    }
}
