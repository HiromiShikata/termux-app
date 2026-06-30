package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserGoogleSignInRedirectTest {

    @Test
    public void accountsGoogleSignInStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.com/signin/v2/identifier"));
    }

    @Test
    public void accountsGoogleLandingPageStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.com/"));
    }

    @Test
    public void oauthTokenEndpointStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://oauth2.googleapis.com/token"));
    }

    @Test
    public void oauthAuthorizePathStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/o/oauth2/v2/auth?client_id=app"));
    }

    @Test
    public void passkeyCeremonyHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://gds.google.com/web/passkeyenrollment"));
    }

    @Test
    public void webauthnChallengePathStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/v3/signin/webauthn/get"));
    }

    @Test
    public void securityChallengePathStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/signin/challenge/az"));
    }

    @Test
    public void signInConsentPathStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/signin/oauth/consent"));
    }

    @Test
    public void youtubeAccountsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.youtube.com/signin"));
    }

    @Test
    public void regionalGoogleAccountsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.co.jp/signin/v2/identifier"));
    }

    @Test
    public void docsSpreadsheetContentStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://docs.google.com/spreadsheets/d/abc123/edit"));
    }

    @Test
    public void docsDocumentContentStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://docs.google.com/document/d/abc123/edit"));
    }

    @Test
    public void sheetsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://sheets.google.com/"));
    }

    @Test
    public void driveContentStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://drive.google.com/drive/my-drive"));
    }

    @Test
    public void googleSearchHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/search?q=test"));
    }

    @Test
    public void googleHomePageStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/"));
    }

    @Test
    public void mailGoogleHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://mail.google.com/"));
    }

    @Test
    public void nonGoogleHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://example.com/page"));
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
}
