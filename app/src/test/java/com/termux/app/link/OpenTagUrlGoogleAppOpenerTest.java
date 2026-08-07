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
public class OpenTagUrlGoogleAppOpenerTest {

    private static final String SESSION_HANDLE = "session-handle-1";

    private final List<String> mUrlsOpenedInTheInAppBrowser = new ArrayList<>();

    private final List<String> mSessionHandlesOpenedInTheInAppBrowser = new ArrayList<>();

    private OpenTagUrlGoogleAppOpener openerFor(Context context) {
        return new OpenTagUrlGoogleAppOpener(context, (sessionHandle, url) -> {
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
