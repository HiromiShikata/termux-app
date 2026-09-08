package com.termux.app.browser;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class TermuxBrowserControllerSlackUrlOpensInChromeTest {

    private static final String CHROME_PACKAGE = "com.android.chrome";

    private static void installChromeFor(Context context, String url) {
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = CHROME_PACKAGE;
        info.activityInfo.name = "com.google.android.apps.chrome.Main";
        info.activityInfo.applicationInfo = new ApplicationInfo();
        info.activityInfo.applicationInfo.packageName = CHROME_PACKAGE;
        ShadowPackageManager shadowPM = Shadows.shadowOf(context.getPackageManager());
        shadowPM.addResolveInfoForIntent(
            new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(CHROME_PACKAGE),
            info);
    }

    @Test
    public void slackUrlOpensSlackWhenSlackIsInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";

        boolean result = TermuxBrowserController.openInNativeAppOrFallBackToChrome(context, url);

        Assert.assertTrue("a Slack URL with a matching native app must be reported as handled", result);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull("a Slack URL must launch the Slack application when it is installed", started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.Slack", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void slackUrlOpensChromeWhenSlackIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";
        installChromeFor(context, url);
        shadowApplication.checkActivities(true);

        boolean result = TermuxBrowserController.openInNativeAppOrFallBackToChrome(context, url);

        Assert.assertTrue("a Slack URL must be reported as handled even when Slack is not installed", result);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull("a Slack URL must launch Chrome when Slack is not installed", started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals(CHROME_PACKAGE, started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void nonNativeAppUrlIsNotHandled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://github.com/HiromiShikata/termux-app/pull/1";

        boolean result = TermuxBrowserController.openInNativeAppOrFallBackToChrome(context, url);

        Assert.assertFalse("a URL with no native app target must not be reported as handled so the caller can decide", result);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }
}
