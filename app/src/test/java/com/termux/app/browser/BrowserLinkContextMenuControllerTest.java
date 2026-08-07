package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BrowserLinkContextMenuControllerTest {

    private static final class RecordingActions implements BrowserLinkContextMenuController.Actions {
        String openedInBrowserUrl;
        String openedInBrowserBackgroundUrl;
        String createdSessionUrl;

        @Override
        public void openLinkInBrowser(@NonNull String linkUrl) {
            openedInBrowserUrl = linkUrl;
        }

        @Override
        public void openLinkInBrowserBackground(@NonNull String linkUrl) {
            openedInBrowserBackgroundUrl = linkUrl;
        }

        @Override
        public void createSessionForLink(@NonNull String linkUrl) {
            createdSessionUrl = linkUrl;
        }
    }

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private AlertDialog showMenuFor(@NonNull WebView webView, @NonNull String linkUrl,
                                    @NonNull BrowserLinkContextMenuController.Actions actions) {
        return showMenuFor(webView, linkUrl, null, actions);
    }

    private AlertDialog showMenuFor(@NonNull WebView webView, @NonNull String linkUrl,
                                    String anchorText,
                                    @NonNull BrowserLinkContextMenuController.Actions actions) {
        BrowserLinkContextMenuController controller =
            new BrowserLinkContextMenuController(context(), webView, actions);
        controller.showLinkContextMenu(linkUrl, anchorText);
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    @Test
    public void attachSetsLongClickListenerOnWebView() {
        WebView webView = new WebView(context());
        new BrowserLinkContextMenuController(context(), webView, new RecordingActions()).attach();
        Assert.assertTrue(webView.isLongClickable());
    }

    @Test
    public void menuTitleIsTheLinkUrl() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", new RecordingActions());
        Assert.assertEquals("https://example.com/page",
            Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void menuListsSixActionsInOrder() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", new RecordingActions());
        ListAdapter adapter = dialog.getListView().getAdapter();
        Assert.assertEquals(6, adapter.getCount());
        Assert.assertEquals(context().getString(R.string.action_browser_copy_link_text), adapter.getItem(0));
        Assert.assertEquals(context().getString(R.string.action_browser_copy_link_url), adapter.getItem(1));
        Assert.assertEquals(context().getString(R.string.action_browser_open_link), adapter.getItem(2));
        Assert.assertEquals(context().getString(R.string.action_browser_open_link_background), adapter.getItem(3));
        Assert.assertEquals(context().getString(R.string.action_browser_open_in_chrome), adapter.getItem(4));
        Assert.assertEquals(context().getString(R.string.action_browser_create_session_for_link), adapter.getItem(5));
    }

    @Test
    public void copyLinkTextActionCopiesAnchorTextToClipboard() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", "Example Page", new RecordingActions());
        dialog.getListView().performItemClick(null, 0, 0);
        Assert.assertEquals("Example Page", clipboardText());
    }

    @Test
    public void copyLinkTextActionFallsBackToUrlWhenAnchorTextIsEmpty() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", "", new RecordingActions());
        dialog.getListView().performItemClick(null, 0, 0);
        Assert.assertEquals("https://example.com/page", clipboardText());
    }

    @Test
    public void copyLinkUrlActionCopiesLinkUrlToClipboard() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", "Example Page", new RecordingActions());
        dialog.getListView().performItemClick(null, 1, 0);
        Assert.assertEquals("https://example.com/page", clipboardText());
    }

    @Test
    public void openInBrowserActionDelegatesToActions() {
        WebView webView = new WebView(context());
        RecordingActions actions = new RecordingActions();
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", actions);
        dialog.getListView().performItemClick(null, 2, 0);
        Assert.assertEquals("https://example.com/page", actions.openedInBrowserUrl);
    }

    @Test
    public void openInBrowserBackgroundActionDelegatesToActions() {
        WebView webView = new WebView(context());
        RecordingActions actions = new RecordingActions();
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", actions);
        dialog.getListView().performItemClick(null, 3, 0);
        Assert.assertEquals("https://example.com/page", actions.openedInBrowserBackgroundUrl);
        Assert.assertNull(actions.openedInBrowserUrl);
    }

    @Test
    public void createSessionActionCopiesUrlAndDelegatesToActions() {
        WebView webView = new WebView(context());
        RecordingActions actions = new RecordingActions();
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", actions);
        dialog.getListView().performItemClick(null, 5, 0);
        Assert.assertEquals("https://example.com/page", actions.createdSessionUrl);
        Assert.assertEquals("https://example.com/page", clipboardText());
    }

    @Test
    public void nonGoogleUrlDoesNotAddOpenInNativeAppItem() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView, "https://example.com/page", new RecordingActions());
        ListAdapter adapter = dialog.getListView().getAdapter();
        for (int index = 0; index < adapter.getCount(); index++) {
            Assert.assertNotEquals(
                context().getString(R.string.action_browser_open_in_native_app, "Google Sheets"),
                adapter.getItem(index));
        }
    }

    @Test
    public void googleSpreadsheetUrlAddsOpenInSheetsItem() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView,
            "https://docs.google.com/spreadsheets/d/abc123/edit", new RecordingActions());
        ListAdapter adapter = dialog.getListView().getAdapter();
        Assert.assertEquals(7, adapter.getCount());
        Assert.assertEquals(
            context().getString(R.string.action_browser_open_in_native_app, "Google Sheets"),
            adapter.getItem(4));
    }

    @Test
    public void googleCalendarUrlAddsOpenInCalendarItem() {
        WebView webView = new WebView(context());
        AlertDialog dialog = showMenuFor(webView,
            "https://calendar.google.com/calendar/u/0/r", new RecordingActions());
        ListAdapter adapter = dialog.getListView().getAdapter();
        Assert.assertEquals(7, adapter.getCount());
        Assert.assertEquals(
            context().getString(R.string.action_browser_open_in_native_app, "Google Calendar"),
            adapter.getItem(4));
    }

    @Test
    public void selectingOpenInNativeAppLaunchesIntentWithMappedPackage() {
        Context context = context();
        WebView webView = new WebView(context);
        RecordingActions actions = new RecordingActions();
        BrowserLinkContextMenuController controller =
            new BrowserLinkContextMenuController(context, webView, actions);
        String url = "https://mail.google.com/mail/u/0/#inbox";
        controller.showLinkContextMenu(url, null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        int index = indexOfItem(dialog,
            context.getString(R.string.action_browser_open_in_native_app, "Gmail"));
        dialog.getListView().performItemClick(null, index, 0);
        Intent started = Shadows.shadowOf((android.app.Application) context).getNextStartedActivity();
        Assert.assertNotNull(started);
        Assert.assertEquals(Intent.ACTION_VIEW, started.getAction());
        Assert.assertEquals("com.google.android.gm", started.getPackage());
        Assert.assertEquals(url, started.getDataString());
        Assert.assertNull(actions.openedInBrowserUrl);
    }

    @Test
    public void selectingOpenInNativeAppFallsBackToBrowserWhenAppNotInstalled() {
        Context context = context();
        ShadowApplication shadowApplication = Shadows.shadowOf((android.app.Application) context);
        shadowApplication.checkActivities(true);
        WebView webView = new WebView(context);
        RecordingActions actions = new RecordingActions();
        BrowserLinkContextMenuController controller =
            new BrowserLinkContextMenuController(context, webView, actions);
        String url = "https://drive.google.com/file/d/abc123/view";
        controller.showLinkContextMenu(url, null);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        int index = indexOfItem(dialog,
            context.getString(R.string.action_browser_open_in_native_app, "Google Drive"));
        dialog.getListView().performItemClick(null, index, 0);
        Assert.assertNull(Shadows.shadowOf((android.app.Application) context).getNextStartedActivity());
        Assert.assertEquals(url, actions.openedInBrowserUrl);
    }

    private int indexOfItem(@NonNull AlertDialog dialog, @NonNull String label) {
        ListAdapter adapter = dialog.getListView().getAdapter();
        for (int index = 0; index < adapter.getCount(); index++) {
            if (label.equals(adapter.getItem(index))) return index;
        }
        throw new AssertionError("menu item not found: " + label);
    }

    private String clipboardText() {
        ClipboardManager clipboardManager =
            (ClipboardManager) context().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null || clipboardManager.getPrimaryClip() == null) return null;
        return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
    }
}
