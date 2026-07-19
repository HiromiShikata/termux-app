package com.termux.app;

import android.content.Context;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TerminalLongPressUrlGoogleAppMenuTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void calendarSubdomainUrlShowsOpenInCalendarTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_google_app, "Google Calendar"),
            TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(),
                "https://calendar.google.com/calendar/u/0/r"));
    }

    @Test
    public void googleCalendarPathUrlShowsOpenInCalendarTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_google_app, "Google Calendar"),
            TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(),
                "https://www.google.com/calendar/render?action=TEMPLATE"));
    }

    @Test
    public void spreadsheetsUrlShowsOpenInSheetsTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_google_app, "Google Sheets"),
            TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(),
                "https://docs.google.com/spreadsheets/d/abc123/edit"));
    }

    @Test
    public void driveUrlShowsOpenInDriveTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_google_app, "Google Drive"),
            TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(),
                "https://drive.google.com/file/d/abc123/view"));
    }

    @Test
    public void nonGoogleUrlHasNoGoogleAppMenuTitle() {
        Assert.assertNull(TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(),
            "https://example.com/page"));
    }

    @Test
    public void nullUrlHasNoGoogleAppMenuTitle() {
        Assert.assertNull(TermuxActivity.longPressedUrlGoogleAppMenuTitle(context(), null));
    }

    @Test
    public void calendarUrlMenuIncludesOpenInGoogleAppItemInOrder() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(), "https://calendar.google.com/calendar/u/0/r");

        Assert.assertEquals(4, items.size());
        assertItem(items.get(0), context().getString(R.string.action_open_link_in_browser));
        assertItem(items.get(1), context().getString(R.string.action_open_link_in_chrome));
        assertItem(items.get(2), context().getString(R.string.action_open_link_in_google_app, "Google Calendar"));
        Assert.assertEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, items.get(2).getMenuItemId());
        assertItem(items.get(3), context().getString(R.string.action_copy_link_url));
    }

    @Test
    public void spreadsheetsUrlMenuIncludesOpenInSheetsItem() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(),
                "https://docs.google.com/spreadsheets/d/abc123/edit");

        Assert.assertEquals(4, items.size());
        assertItem(items.get(2), context().getString(R.string.action_open_link_in_google_app, "Google Sheets"));
        Assert.assertEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, items.get(2).getMenuItemId());
    }

    @Test
    public void nonGoogleUrlMenuOmitsGoogleAppItem() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(), "https://example.com/page");

        Assert.assertEquals(3, items.size());
        for (TermuxActivity.LongPressedUrlMenuItem item : items) {
            Assert.assertNotEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, item.getMenuItemId());
        }
    }

    @Test
    public void nullUrlMenuHasNoItems() {
        Assert.assertTrue(TermuxActivity.longPressedUrlMenuItems(context(), null).isEmpty());
    }

    private void assertItem(TermuxActivity.LongPressedUrlMenuItem item, CharSequence expectedTitle) {
        Assert.assertEquals(expectedTitle, item.getTitle());
    }
}
