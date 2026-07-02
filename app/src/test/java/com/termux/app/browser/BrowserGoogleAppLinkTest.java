package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserGoogleAppLinkTest {

    private void assertTarget(String url, String expectedLabel, String expectedPackage) {
        BrowserGoogleAppLink.GoogleAppTarget target = BrowserGoogleAppLink.resolveTarget(url);
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
    public void calendarUrlMapsToCalendar() {
        assertTarget("https://calendar.google.com/calendar/u/0/r",
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
        Assert.assertNull(BrowserGoogleAppLink.resolveTarget("https://docs.google.com/"));
    }

    @Test
    public void googleSearchWithoutMapsPathHasNoTarget() {
        Assert.assertNull(BrowserGoogleAppLink.resolveTarget("https://www.google.com/search?q=test"));
    }

    @Test
    public void nonGoogleUrlHasNoTarget() {
        Assert.assertNull(BrowserGoogleAppLink.resolveTarget("https://example.com/spreadsheets/d/abc"));
    }

    @Test
    public void nonHttpSchemeHasNoTarget() {
        Assert.assertNull(BrowserGoogleAppLink.resolveTarget("ftp://drive.google.com/file"));
    }

    @Test
    public void nullUrlHasNoTarget() {
        Assert.assertNull(BrowserGoogleAppLink.resolveTarget(null));
    }

    @Test
    public void hostMatchingIsCaseInsensitive() {
        assertTarget("https://Drive.Google.com/file/d/abc123/view",
            "Google Drive", "com.google.android.apps.docs");
    }
}
