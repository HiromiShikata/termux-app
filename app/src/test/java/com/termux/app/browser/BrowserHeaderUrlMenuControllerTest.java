package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
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

@RunWith(RobolectricTestRunner.class)
public class BrowserHeaderUrlMenuControllerTest {

    private static final class RecordingActions implements BrowserHeaderUrlMenuController.Actions {
        String createdSessionUrl;

        @Override
        public void createSessionForUrl(@NonNull String url) {
            createdSessionUrl = url;
        }
    }

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private AlertDialog showMenuFor(@NonNull String url,
                                    @NonNull BrowserHeaderUrlMenuController.Actions actions) {
        BrowserHeaderUrlMenuController controller =
            new BrowserHeaderUrlMenuController(context(), actions);
        controller.showHeaderUrlMenu(url);
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    @Test
    public void menuTitleIsTheCurrentUrl() {
        AlertDialog dialog = showMenuFor("https://example.com/page", new RecordingActions());
        Assert.assertEquals("https://example.com/page", Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void menuListsThreeActionsInOrder() {
        AlertDialog dialog = showMenuFor("https://example.com/page", new RecordingActions());
        ListAdapter adapter = dialog.getListView().getAdapter();
        Assert.assertEquals(3, adapter.getCount());
        Assert.assertEquals(context().getString(R.string.action_browser_copy_url), adapter.getItem(0));
        Assert.assertEquals(context().getString(R.string.action_browser_new_session), adapter.getItem(1));
        Assert.assertEquals(context().getString(R.string.action_browser_open_in_chrome), adapter.getItem(2));
    }

    @Test
    public void copyActionCopiesCurrentUrlToClipboard() {
        AlertDialog dialog = showMenuFor("https://example.com/page", new RecordingActions());
        dialog.getListView().performItemClick(null, 0, 0);
        Assert.assertEquals("https://example.com/page", clipboardText());
    }

    @Test
    public void newSessionActionDelegatesToActions() {
        RecordingActions actions = new RecordingActions();
        AlertDialog dialog = showMenuFor("https://example.com/page", actions);
        dialog.getListView().performItemClick(null, 1, 0);
        Assert.assertEquals("https://example.com/page", actions.createdSessionUrl);
    }

    @Test
    public void doesNotShowMenuForAboutBlank() {
        boolean shown = new BrowserHeaderUrlMenuController(context(), new RecordingActions())
            .showHeaderUrlMenu("about:blank");
        Assert.assertFalse(shown);
        Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void doesNotShowMenuForNull() {
        boolean shown = new BrowserHeaderUrlMenuController(context(), new RecordingActions())
            .showHeaderUrlMenu(null);
        Assert.assertFalse(shown);
        Assert.assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    private String clipboardText() {
        ClipboardManager clipboardManager =
            (ClipboardManager) context().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null || clipboardManager.getPrimaryClip() == null) return null;
        return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
    }
}
