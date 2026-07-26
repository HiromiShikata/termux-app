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
public class GoogleAppLinkTest {

    private void assertTarget(String url, String expectedLabel, String expectedPackage) {
        GoogleAppLink.GoogleAppTarget target = GoogleAppLink.resolveTarget(url);
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
        Assert.assertNull(GoogleAppLink.resolveTarget("https://docs.google.com/"));
    }

    @Test
    public void googleSearchWithoutRecognizedPathHasNoTarget() {
        Assert.assertNull(GoogleAppLink.resolveTarget("https://www.google.com/search?q=test"));
    }

    @Test
    public void nonGoogleUrlHasNoTarget() {
        Assert.assertNull(GoogleAppLink.resolveTarget("https://example.com/spreadsheets/d/abc"));
    }

    @Test
    public void nonHttpSchemeHasNoTarget() {
        Assert.assertNull(GoogleAppLink.resolveTarget("ftp://drive.google.com/file"));
    }

    @Test
    public void nullUrlHasNoTarget() {
        Assert.assertNull(GoogleAppLink.resolveTarget(null));
    }

    @Test
    public void hostMatchingIsCaseInsensitive() {
        assertTarget("https://Drive.Google.com/file/d/abc123/view",
            "Google Drive", "com.google.android.apps.docs");
    }

    @Test
    public void openInGoogleAppStartsViewIntentWithMappedPackage() {
        Context context = RuntimeEnvironment.getApplication();
        String url = "https://calendar.google.com/calendar/u/0/r";
        GoogleAppLink.GoogleAppTarget target = GoogleAppLink.resolveTarget(url);
        Assert.assertNotNull(target);

        boolean launched = GoogleAppLink.openInGoogleApp(context, url, target);

        Assert.assertTrue(launched);
        Intent started = Shadows.shadowOf((Application) context).getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.calendar", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInGoogleAppReturnsFalseWhenAppNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://docs.google.com/spreadsheets/d/abc123/edit";
        GoogleAppLink.GoogleAppTarget target = GoogleAppLink.resolveTarget(url);
        Assert.assertNotNull(target);

        boolean launched = GoogleAppLink.openInGoogleApp(context, url, target);

        Assert.assertFalse(launched);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInGoogleAppOrElseOpensNativeAppForResolvableInstalledUrl() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://calendar.google.com/calendar/u/0/r";
        boolean[] fallbackRan = {false};

        GoogleAppLink.openInGoogleAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertFalse("browser fallback must not run when the native app is installed", fallbackRan[0]);
        Intent started = shadowApplication.getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.calendar", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
    }

    @Test
    public void openInGoogleAppOrElseFallsBackToBrowserWhenNativeAppNotInstalled() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        shadowApplication.checkActivities(true);
        String url = "https://docs.google.com/spreadsheets/d/abc123/edit";
        boolean[] fallbackRan = {false};

        GoogleAppLink.openInGoogleAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run when the native app is not installed", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }

    @Test
    public void openInGoogleAppOrElseFallsBackToBrowserForNonResolvableUrl() {
        Context context = RuntimeEnvironment.getApplication();
        ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
        String url = "https://example.com/some/page";
        boolean[] fallbackRan = {false};

        GoogleAppLink.openInGoogleAppOrElse(context, url, () -> fallbackRan[0] = true);

        Assert.assertTrue("browser fallback must run for a URL with no matching native app", fallbackRan[0]);
        Assert.assertNull(shadowApplication.getNextStartedActivity());
    }
}
