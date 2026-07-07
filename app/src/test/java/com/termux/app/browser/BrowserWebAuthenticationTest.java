package com.termux.app.browser;

import android.app.Application;
import android.webkit.WebView;

import androidx.webkit.WebSettingsCompat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BrowserWebAuthenticationTest {

    private WebView newWebView() {
        return new WebView(RuntimeEnvironment.getApplication());
    }

    private ShadowApplication shadowApplication() {
        return Shadows.shadowOf((Application) RuntimeEnvironment.getApplication());
    }

    @Test
    public void enablesForBrowserOnlyWhenFeatureSupportedAndPermissionGranted() {
        Assert.assertTrue(BrowserWebAuthentication.shouldEnableForBrowser(true, true));
    }

    @Test
    public void staysDisabledWhenFeatureSupportedButPermissionNotGranted() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForBrowser(true, false));
    }

    @Test
    public void staysDisabledWhenPermissionGrantedButFeatureNotSupported() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForBrowser(false, true));
    }

    @Test
    public void staysDisabledWhenNeitherFeatureSupportedNorPermissionGranted() {
        Assert.assertFalse(BrowserWebAuthentication.shouldEnableForBrowser(false, false));
    }

    @Test
    public void requiresTheWebAuthenticationWebViewFeature() {
        Assert.assertEquals("WEB_AUTHENTICATION", BrowserWebAuthentication.REQUIRED_FEATURE);
    }

    @Test
    public void requiresTheCredentialManagerSetOriginPermission() {
        Assert.assertEquals(
            "android.permission.CREDENTIAL_MANAGER_SET_ORIGIN",
            BrowserWebAuthentication.REQUIRED_PERMISSION);
    }

    @Test
    public void usesBrowserSupportLevelSoArbitrarySiteOriginsAreAllowed() {
        Assert.assertEquals(
            WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER,
            BrowserWebAuthentication.SUPPORT_LEVEL);
    }

    @Test
    public void applyDoesNotThrowWhenPermissionNotGranted() {
        shadowApplication().denyPermissions(BrowserWebAuthentication.REQUIRED_PERMISSION);
        BrowserWebAuthentication.apply(newWebView());
    }

    @Test
    public void applyDoesNotThrowWhenPermissionGranted() {
        shadowApplication().grantPermissions(BrowserWebAuthentication.REQUIRED_PERMISSION);
        BrowserWebAuthentication.apply(newWebView());
    }
}
