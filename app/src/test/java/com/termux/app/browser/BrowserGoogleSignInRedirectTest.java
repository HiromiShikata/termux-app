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
    public void accountsGoogleLandingPageRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.com/"));
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
    public void docsPresentationContentStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://docs.google.com/presentation/d/abc123/edit"));
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
    public void slidesHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://slides.google.com/"));
    }

    @Test
    public void formsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
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
        Assert.assertEquals(1, BrowserGoogleSignInRedirect.externalBrowserHosts().size());
    }

    @Test
    public void externalBrowserHostsListExcludesDocumentHosts() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.externalBrowserHosts()
            .contains("docs.google.com"));
        Assert.assertFalse(BrowserGoogleSignInRedirect.externalBrowserHosts()
            .contains("drive.google.com"));
    }

    @Test
    public void oauthTokenEndpointRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://oauth2.googleapis.com/token"));
    }

    @Test
    public void oauthAuthorizeEndpointRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/o/oauth2/v2/auth?client_id=app"));
    }

    @Test
    public void youtubeAccountsHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.youtube.com/signin"));
    }

    @Test
    public void regionalGoogleAccountsHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.co.jp/signin/v2/identifier"));
    }

    @Test
    public void regionalGoogleAccountsSingleLabelTopLevelRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.de/signin"));
    }

    @Test
    public void passkeyCeremonyHostRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://gds.google.com/web/passkeyenrollment"));
    }

    @Test
    public void webauthnChallengePathRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/v3/signin/webauthn/get"));
    }

    @Test
    public void googleSecurityChallengePathRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/signin/challenge/az"));
    }

    @Test
    public void googleSignInSubPathRoutesExternal() {
        Assert.assertTrue(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://content.google.com/signin/oauth/consent"));
    }

    @Test
    public void googleHomePageStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/"));
    }

    @Test
    public void googleMapsStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://www.google.com/maps/place/somewhere"));
    }

    @Test
    public void nonGoogleWebauthnPathStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://example.com/account/webauthn/register"));
    }

    @Test
    public void regionalLookalikeAccountsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.co.jp.evil.example/signin"));
    }

    @Test
    public void longRegionalLookalikeAccountsHostStaysInWebView() {
        Assert.assertFalse(BrowserGoogleSignInRedirect.requiresExternalBrowser(
            "https://accounts.google.example.com/signin"));
    }
}
