package com.termux.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityOwnerCallDialogUrlOpensInMatchingAppTest {

    @Test
    public void slackUrlOpensSlackApplicationInsteadOfFallingBackToBrowser() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";
        boolean[] fallbackRan = {false};

        TermuxActivity.openOwnerCallUrlInMatchingAppOrBrowser(context, url, () -> fallbackRan[0] = true);

        Assert.assertFalse("browser fallback must not run when Slack is installed", fallbackRan[0]);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.Slack", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void slackUrlFallsBackToBrowserWhenSlackIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";
        boolean[] fallbackRan = {false};

        TermuxActivity.openOwnerCallUrlInMatchingAppOrBrowser(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run when Slack is not installed", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void nonNativeAppUrlFallsBackToBrowser() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://github.com/HiromiShikata/termux-app";
        boolean[] fallbackRan = {false};

        TermuxActivity.openOwnerCallUrlInMatchingAppOrBrowser(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run for a URL with no matching native app", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }
}
