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
public class TerminalLongPressUrlNativeAppMenuTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void calendarSubdomainUrlShowsOpenInCalendarTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_native_app, "Google Calendar"),
            TermuxActivity.longPressedUrlNativeAppMenuTitle(context(),
                "https://calendar.google.com/calendar/u/0/r"));
    }

    @Test
    public void googleCalendarPathUrlShowsOpenInCalendarTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_native_app, "Google Calendar"),
            TermuxActivity.longPressedUrlNativeAppMenuTitle(context(),
                "https://www.google.com/calendar/render?action=TEMPLATE"));
    }

    @Test
    public void spreadsheetsUrlShowsOpenInSheetsTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_native_app, "Google Sheets"),
            TermuxActivity.longPressedUrlNativeAppMenuTitle(context(),
                "https://docs.google.com/spreadsheets/d/abc123/edit"));
    }

    @Test
    public void driveUrlShowsOpenInDriveTitle() {
        Assert.assertEquals(
            context().getString(R.string.action_open_link_in_native_app, "Google Drive"),
            TermuxActivity.longPressedUrlNativeAppMenuTitle(context(),
                "https://drive.google.com/file/d/abc123/view"));
    }

    @Test
    public void nonGoogleUrlHasNoNativeAppMenuTitle() {
        Assert.assertNull(TermuxActivity.longPressedUrlNativeAppMenuTitle(context(),
            "https://example.com/page"));
    }

    @Test
    public void nullUrlHasNoNativeAppMenuTitle() {
        Assert.assertNull(TermuxActivity.longPressedUrlNativeAppMenuTitle(context(), null));
    }

    @Test
    public void calendarUrlMenuIncludesOpenInNativeAppItemInOrder() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(), "https://calendar.google.com/calendar/u/0/r");

        Assert.assertEquals(5, items.size());
        assertItem(items.get(0), context().getString(R.string.action_open_link_in_browser));
        assertItem(items.get(1), context().getString(R.string.action_open_link_in_browser_background));
        assertItem(items.get(2), context().getString(R.string.action_open_link_in_chrome));
        assertItem(items.get(3), context().getString(R.string.action_open_link_in_native_app, "Google Calendar"));
        Assert.assertEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, items.get(3).getMenuItemId());
        assertItem(items.get(4), context().getString(R.string.action_copy_link_url));
    }

    @Test
    public void spreadsheetsUrlMenuIncludesOpenInSheetsItem() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(),
                "https://docs.google.com/spreadsheets/d/abc123/edit");

        Assert.assertEquals(5, items.size());
        assertItem(items.get(3), context().getString(R.string.action_open_link_in_native_app, "Google Sheets"));
        Assert.assertEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, items.get(3).getMenuItemId());
    }

    @Test
    public void nonGoogleUrlMenuOmitsNativeAppItem() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(), "https://example.com/page");

        Assert.assertEquals(4, items.size());
        for (TermuxActivity.LongPressedUrlMenuItem item : items) {
            Assert.assertNotEquals(TermuxActivity.CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, item.getMenuItemId());
        }
    }

    @Test
    public void nonGoogleUrlMenuIncludesOpenInBrowserBackgroundItemSecond() {
        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context(), "https://example.com/page");

        Assert.assertEquals(4, items.size());
        assertItem(items.get(0), context().getString(R.string.action_open_link_in_browser));
        assertItem(items.get(1), context().getString(R.string.action_open_link_in_browser_background));
    }

    @Test
    public void nullUrlMenuHasNoItems() {
        Assert.assertTrue(TermuxActivity.longPressedUrlMenuItems(context(), null).isEmpty());
    }

    private void assertItem(TermuxActivity.LongPressedUrlMenuItem item, CharSequence expectedTitle) {
        Assert.assertEquals(expectedTitle, item.getTitle());
    }
}
