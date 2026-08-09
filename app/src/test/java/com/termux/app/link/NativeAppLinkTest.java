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

@RunWith(RobolectricTestRunner.class)
public class NativeAppLinkTest {

    private void assertTarget(String url, String expectedLabel, String expectedPackage) {
        NativeAppLink.NativeAppTarget target = NativeAppLink.resolveTarget(url);
        Assert.assertNotNull("expected a target for " + url, target);
        Assert.assertEquals(expectedLabel, target.getAppDisplayName());
        Assert.assertEquals(expectedPackage, target.getPackageName());
    }

    @Test
    public void spreadsheetsUrlMapsToSheets() {
        assertTarget("https://docs.google.com/spreadsheets/d/abc123/edit",
            "Google Sheets", "com.google.android.apps.docs.editors.sheets");
    }

    @Test
    public void documentUrlMapsToDocs() {
        assertTarget("https://docs.google.com/document/d/abc123/edit",
            "Google Docs", "com.google.android.apps.docs.editors.docs");
    }

    @Test
    public void presentationUrlMapsToSlides() {
        assertTarget("https://docs.google.com/presentation/d/abc123/edit",
            "Google Slides", "com.google.android.apps.docs.editors.slides");
    }

    @Test
    public void driveUrlMapsToDrive() {
        assertTarget("https://drive.google.com/file/d/abc123/view",
            "Google Drive", "com.google.android.apps.docs");
    }

    @Test
    public void meetUrlMapsToMeet() {
        assertTarget("https://meet.google.com/abc-defg-hij",
            "Google Meet", "com.google.android.apps.tachyon");
    }

    @Test
    public void calendarSubdomainUrlMapsToCalendar() {
        assertTarget("https://calendar.google.com/calendar/u/0/r",
            "Google Calendar", "com.google.android.calendar");
    }

    @Test
    public void calendarEventUrlMapsToCalendar() {
        assertTarget("https://calendar.google.com/calendar/u/0/r/eventedit/abc123",
            "Google Calendar", "com.google.android.calendar");
    }

    @Test
    public void googleCalendarPathUrlMapsToCalendar() {
        assertTarget("https://www.google.com/calendar/render?action=TEMPLATE",
            "Google Calendar", "com.google.android.calendar");
    }

    @Test
    public void mailUrlMapsToGmail() {
        assertTarget("https://mail.google.com/mail/u/0/#inbox",
            "Gmail", "com.google.android.gm");
    }

    @Test
    public void chatSpacePermalinkMapsToGoogleChat() {
        assertTarget("https://chat.google.com/room/AAAAabcdefg/BBBBhijklmn",
            "Google Chat", "com.google.android.apps.dynamite");
    }

    @Test
    public void chatDirectMessageUrlMapsToGoogleChat() {
        assertTarget("https://chat.google.com/dm/AAAAabcdefg",
            "Google Chat", "com.google.android.apps.dynamite");
    }

    @Test
    public void chatWebClientRootUrlMapsToGoogleChat() {
        assertTarget("https://chat.google.com/",
            "Google Chat", "com.google.android.apps.dynamite");
    }

    @Test
    public void chatHostMatchingIsCaseInsensitive() {
        assertTarget("https://Chat.Google.com/room/AAAAabcdefg/BBBBhijklmn",
            "Google Chat", "com.google.android.apps.dynamite");
    }

    @Test
    public void photosUrlMapsToPhotos() {
        assertTarget("https://photos.google.com/photo/abc123",
            "Google Photos", "com.google.android.apps.photos");
    }

    @Test
    public void keepUrlMapsToKeep() {
        assertTarget("https://keep.google.com/#home",
            "Google Keep", "com.google.android.keep");
    }

    @Test
    public void mapsSubdomainUrlMapsToMaps() {
        assertTarget("https://maps.google.com/maps?q=tokyo",
            "Google Maps", "com.google.android.apps.maps");
    }

    @Test
    public void googleMapsPathUrlMapsToMaps() {
        assertTarget("https://www.google.com/maps/place/Tokyo",
            "Google Maps", "com.google.android.apps.maps");
    }

    @Test
    public void docsWithoutRecognizedEditorPathHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://docs.google.com/"));
    }

    @Test
    public void googleSearchWithoutRecognizedPathHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://www.google.com/search?q=test"));
    }

    @Test
    public void nonGoogleUrlHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://example.com/spreadsheets/d/abc"));
    }

    @Test
    public void nonHttpSchemeHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("ftp://drive.google.com/file"));
    }

    @Test
    public void nullUrlHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget(null));
    }

    @Test
    public void hostMatchingIsCaseInsensitive() {
        assertTarget("https://Drive.Google.com/file/d/abc123/view",
            "Google Drive", "com.google.android.apps.docs");
    }

    @Test
    public void slackThreadPermalinkMapsToSlack() {
        assertTarget("https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000"
                + "?thread_ts=1700000000.000000&cid=C01ABCDEFGH",
            "Slack", "com.Slack");
    }

    @Test
    public void slackMessagePermalinkWithoutThreadQueryMapsToSlack() {
        assertTarget("https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000",
            "Slack", "com.Slack");
    }

    @Test
    public void slackWebClientThreadUrlMapsToSlack() {
        assertTarget("https://app.slack.com/client/T01ABCDEFGH/C01ABCDEFGH/thread/C01ABCDEFGH-1700000000.000000",
            "Slack", "com.Slack");
    }

    @Test
    public void slackHostMatchingIsCaseInsensitive() {
        assertTarget("https://A-Workspace.Slack.com/archives/C01ABCDEFGH/p1700000000000000",
            "Slack", "com.Slack");
    }

    @Test
    public void slackApiDocumentationUrlHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://api.slack.com/methods/chat.postMessage"));
    }

    @Test
    public void slackMarketingSiteUrlHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://slack.com/intl/en-gb/downloads"));
        Assert.assertNull(NativeAppLink.resolveTarget("https://www.slack.com/help"));
    }

    @Test
    public void slackWorkspaceHostWithoutAPermalinkPathHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://a-workspace.slack.com/home"));
    }

    @Test
    public void slackWebClientHostWithoutAClientPathHasNoTarget() {
        Assert.assertNull(NativeAppLink.resolveTarget("https://app.slack.com/signin"));
    }

    @Test
    public void openInNativeAppOrElseOpensSlackForAThreadPermalink() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000"
            + "?thread_ts=1700000000.000000&cid=C01ABCDEFGH";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertFalse("browser fallback must not run when Slack is installed", fallbackRan[0]);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.Slack", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInNativeAppOrElseFallsBackToBrowserForASlackThreadPermalinkWhenSlackIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://a-workspace.slack.com/archives/C01ABCDEFGH/p1700000000000000";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run when Slack is not installed", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInNativeAppOrElseOpensGoogleChatForASpacePermalink() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://chat.google.com/room/AAAAabcdefg/BBBBhijklmn";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertFalse("browser fallback must not run when Google Chat is installed", fallbackRan[0]);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.apps.dynamite", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInNativeAppOrElseFallsBackToBrowserForAGoogleChatUrlWhenGoogleChatIsNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://chat.google.com/room/AAAAabcdefg/BBBBhijklmn";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run when Google Chat is not installed", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInNativeAppStartsViewIntentWithMappedPackage() {
        Context context = RuntimeEnvironment.getApplication();
        String url = "https://calendar.google.com/calendar/u/0/r";
        NativeAppLink.NativeAppTarget target = NativeAppLink.resolveTarget(url);
        Assert.assertNotNull(target);

        boolean launched = NativeAppLink.openInNativeApp(context, url, target);

        Assert.assertTrue(launched);
        Intent started = Shadows.shadowOf((Application) context).getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.calendar", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInNativeAppReturnsFalseWhenAppNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://docs.google.com/spreadsheets/d/abc123/edit";
        NativeAppLink.NativeAppTarget target = NativeAppLink.resolveTarget(url);
        Assert.assertNotNull(target);

        boolean launched = NativeAppLink.openInNativeApp(context, url, target);

        Assert.assertFalse(launched);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInNativeAppOrElseOpensNativeAppForResolvableInstalledUrl() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://calendar.google.com/calendar/u/0/r";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertFalse("browser fallback must not run when the native app is installed", fallbackRan[0]);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.calendar", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInNativeAppOrElseFallsBackToBrowserWhenNativeAppNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://docs.google.com/spreadsheets/d/abc123/edit";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run when the native app is not installed", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInNativeAppOrElseFallsBackToBrowserForNonResolvableUrl() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://example.com/some/page";
        boolean[] fallbackRan = {false};

        NativeAppLink.openInNativeAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run for a URL with no matching native app", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }
}
