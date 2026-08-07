package com.termux.app.link;

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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OpenTagUrlNativeAppOpenerTest {

    private static final String SESSION_HANDLE = "session-handle-1";

    private final List<String> mUrlsOpenedInTheInAppBrowser = new ArrayList<>();

    private final List<String> mSessionHandlesOpenedInTheInAppBrowser = new ArrayList<>();

    private OpenTagUrlNativeAppOpener openerFor(Context context) {
        return new OpenTagUrlNativeAppOpener(context, (sessionHandle, url) -> {
            mSessionHandlesOpenedInTheInAppBrowser.add(sessionHandle);
            mUrlsOpenedInTheInAppBrowser.add(url);
        });
    }

    @Test
    public void driveUrlOpensTheGoogleDriveApplicationInsteadOfTheInAppBrowser() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://drive.google.com/file/d/abc123/view";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertTrue("a Google Drive link must not open in the in-app browser when the application is installed",
            mUrlsOpenedInTheInAppBrowser.isEmpty());
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.apps.docs", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void driveUrlFallsBackToTheInAppBrowserWhenTheApplicationIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://drive.google.com/file/d/abc123/view";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertEquals(java.util.Collections.singletonList(url), mUrlsOpenedInTheInAppBrowser);
        Assert.assertEquals(java.util.Collections.singletonList(SESSION_HANDLE),
            mSessionHandlesOpenedInTheInAppBrowser);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void slackThreadUrlOpensTheSlackApplicationInsteadOfTheInAppBrowser() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000"
            + "?thread_ts=1700000000.000000&cid=C01ABCDEFGH";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertTrue("a Slack thread link delivered by the open tag must not open in the in-app browser"
                + " when the Slack application is installed",
            mUrlsOpenedInTheInAppBrowser.isEmpty());
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.Slack", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void slackThreadUrlFallsBackToTheInAppBrowserWhenSlackIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertEquals(java.util.Collections.singletonList(url), mUrlsOpenedInTheInAppBrowser);
        Assert.assertEquals(java.util.Collections.singletonList(SESSION_HANDLE),
            mSessionHandlesOpenedInTheInAppBrowser);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void nonGoogleUrlOpensInTheInAppBrowserForTheSameSession() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://github.com/HiromiShikata/termux-app/pull/1";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertEquals(java.util.Collections.singletonList(url), mUrlsOpenedInTheInAppBrowser);
        Assert.assertEquals(java.util.Collections.singletonList(SESSION_HANDLE),
            mSessionHandlesOpenedInTheInAppBrowser);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void spreadsheetUrlOpensTheGoogleSheetsApplication() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://docs.google.com/spreadsheets/d/abc123/edit";

        openerFor(context).openUrlInTabForSession(SESSION_HANDLE, url);

        Assert.assertTrue(mUrlsOpenedInTheInAppBrowser.isEmpty());
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals("com.google.android.apps.docs.editors.sheets", started.getPackage());
    }
}
