package com.termux.shared.termux;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.termux.shared.termux.TermuxUtils.AppInfoMode;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TermuxUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getTermuxPackageContextReturnsNullWhenTermuxNotInstalled() {
        Assert.assertNull(TermuxUtils.getTermuxPackageContext(context()));
    }

    @Test
    public void getTermuxPluginPackageContextsReturnNullWhenNotInstalled() {
        Context context = context();
        Assert.assertNull(TermuxUtils.getTermuxPackageContextWithCode(context));
        Assert.assertNull(TermuxUtils.getTermuxAPIPackageContext(context));
        Assert.assertNull(TermuxUtils.getTermuxBootPackageContext(context));
        Assert.assertNull(TermuxUtils.getTermuxFloatPackageContext(context));
        Assert.assertNull(TermuxUtils.getTermuxStylingPackageContext(context));
        Assert.assertNull(TermuxUtils.getTermuxTaskerPackageContext(context));
        Assert.assertNull(TermuxUtils.getTermuxWidgetPackageContext(context));
    }

    @Test
    public void getContextForPackageOrExitAppReturnsNullWithoutExit() {
        Assert.assertNull(TermuxUtils.getContextForPackageOrExitApp(context(),
            TermuxConstants.TERMUX_PACKAGE_NAME, false));
    }

    @Test
    public void isTermuxAppInstalledReturnsErrorMessageWhenNotInstalled() {
        Assert.assertNotNull(TermuxUtils.isTermuxAppInstalled(context()));
    }

    @Test
    public void isTermuxAPIAppInstalledReturnsErrorMessageWhenNotInstalled() {
        Assert.assertNotNull(TermuxUtils.isTermuxAPIAppInstalled(context()));
    }

    @Test
    public void isTermuxAppAccessibleReturnsErrorMessageWhenNotInstalled() {
        Assert.assertNotNull(TermuxUtils.isTermuxAppAccessible(context()));
    }

    @Test
    public void isUriDataForTermuxOrPluginPackageMatchesTermuxAndPlugins() {
        Assert.assertTrue(TermuxUtils.isUriDataForTermuxOrPluginPackage(
            Uri.parse("package:" + TermuxConstants.TERMUX_PACKAGE_NAME)));
        Assert.assertTrue(TermuxUtils.isUriDataForTermuxOrPluginPackage(
            Uri.parse("package:" + TermuxConstants.TERMUX_PACKAGE_NAME + ".api")));
    }

    @Test
    public void isUriDataForTermuxOrPluginPackageRejectsOtherPackages() {
        Assert.assertFalse(TermuxUtils.isUriDataForTermuxOrPluginPackage(Uri.parse("package:com.example.other")));
    }

    @Test
    public void isUriDataForTermuxPluginPackageMatchesOnlyPlugins() {
        Assert.assertTrue(TermuxUtils.isUriDataForTermuxPluginPackage(
            Uri.parse("package:" + TermuxConstants.TERMUX_PACKAGE_NAME + ".boot")));
        Assert.assertFalse(TermuxUtils.isUriDataForTermuxPluginPackage(
            Uri.parse("package:" + TermuxConstants.TERMUX_PACKAGE_NAME)));
    }

    @Test
    public void getApkReleaseReturnsNullStringForNull() {
        Assert.assertEquals("null", TermuxUtils.getAPKRelease(null));
    }

    @Test
    public void getApkReleaseMapsFdroidDigest() {
        Assert.assertEquals(TermuxConstants.APK_RELEASE_FDROID,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST));
    }

    @Test
    public void getApkReleaseMapsGithubDigestCaseInsensitively() {
        Assert.assertEquals(TermuxConstants.APK_RELEASE_GITHUB,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST.toLowerCase()));
    }

    @Test
    public void getApkReleaseMapsGooglePlayStoreDigest() {
        Assert.assertEquals(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST));
    }

    @Test
    public void getApkReleaseMapsTermuxDevsDigest() {
        Assert.assertEquals(TermuxConstants.APK_RELEASE_TERMUX_DEVS,
            TermuxUtils.getAPKRelease(TermuxConstants.APK_RELEASE_TERMUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST));
    }

    @Test
    public void getApkReleaseReturnsUnknownForUnrecognizedDigest() {
        Assert.assertEquals("Unknown", TermuxUtils.getAPKRelease("0123456789ABCDEF"));
    }

    @Test
    public void getAppInfoMarkdownStringReturnsNullStringForNullContext() {
        Assert.assertEquals("null", TermuxUtils.getAppInfoMarkdownString(null, false));
    }

    @Test
    public void getAppInfoMarkdownStringForContextContainsAppInfoHeading() {
        String markdown = TermuxUtils.getAppInfoMarkdownString(context(), false);
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("App Info"));
    }

    @Test
    public void getAppInfoMarkdownStringWithTermuxInfoTooForContext() {
        String markdown = TermuxUtils.getAppInfoMarkdownString(context(), true);
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("App Info"));
    }

    @Test
    public void getAppInfoMarkdownStringWithModeReturnsNullForNullMode() {
        Assert.assertNull(TermuxUtils.getAppInfoMarkdownString(context(), (AppInfoMode) null));
    }

    @Test
    public void getAppInfoMarkdownStringWithTermuxPackageMode() {
        String markdown = TermuxUtils.getAppInfoMarkdownString(context(), AppInfoMode.TERMUX_PACKAGE);
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("App Info"));
    }

    @Test
    public void getAppInfoMarkdownStringWithTermuxAndPluginPackageMode() {
        String markdown = TermuxUtils.getAppInfoMarkdownString(context(), AppInfoMode.TERMUX_AND_PLUGIN_PACKAGE);
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("App Info"));
    }

    @Test
    public void getAppInfoMarkdownStringInnerForContextIsNonEmpty() {
        String markdown = TermuxUtils.getAppInfoMarkdownStringInner(context());
        Assert.assertNotNull(markdown);
        Assert.assertFalse(markdown.isEmpty());
    }

    @Test
    public void getReportIssueMarkdownStringContainsHeading() {
        String markdown = TermuxUtils.getReportIssueMarkdownString(context());
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("Where To Report An Issue"));
    }

    @Test
    public void getImportantLinksMarkdownStringContainsHeading() {
        String markdown = TermuxUtils.getImportantLinksMarkdownString(context());
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("Important Links"));
    }

    @Test
    public void getTermuxAppApkClassFieldReturnsNullWhenTermuxNotInstalled() {
        Assert.assertNull(TermuxUtils.getTermuxAppAPKClassField(context(),
            "com.termux.app.BuildConfig", "VERSION_NAME"));
    }

    @Test
    public void getTermuxAppApkBuildConfigClassFieldReturnsNullWhenTermuxNotInstalled() {
        Assert.assertNull(TermuxUtils.getTermuxAppAPKBuildConfigClassField(context(), "VERSION_NAME"));
    }

    @Test
    public void getTermuxAppPidReturnsNullWhenTermuxNotRunning() {
        Assert.assertNull(TermuxUtils.getTermuxAppPID(context()));
    }

    @Test
    public void sendTermuxOpenedBroadcastDoesNotThrow() {
        TermuxUtils.sendTermuxOpenedBroadcast(context());
    }
}
