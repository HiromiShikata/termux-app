package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
public class BrowserUrlActionsTest {

    private static final class RecordingHost implements BrowserUrlActions.Host {
        @Nullable
        String currentUrl;
        boolean showedNoUrlMessage;
        String navigatedUrl;
        String createdSessionUrl;
        int addBookmarkCalls;
        int showBookmarksCalls;

        RecordingHost(@Nullable String currentUrl) {
            this.currentUrl = currentUrl;
        }

        @Override
        public String currentPageUrl() {
            return currentUrl;
        }

        @Override
        public void showNoCurrentUrlMessage() {
            showedNoUrlMessage = true;
        }

        @Override
        public void navigateToUrl(@NonNull String editedUrl) {
            navigatedUrl = editedUrl;
        }

        @Override
        public void createSessionForUrl(@NonNull String editedUrl) {
            createdSessionUrl = editedUrl;
        }

        @Override
        public void addCurrentPageBookmark() {
            addBookmarkCalls++;
        }

        @Override
        public void showBookmarksList() {
            showBookmarksCalls++;
        }
    }

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private AlertDialog promptFor(@NonNull RecordingHost host) {
        new BrowserUrlActions(context(), host).promptEditCurrentPageUrl();
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private Button button(@NonNull AlertDialog dialog, int id) {
        return dialog.findViewById(id);
    }

    @Test
    public void doesNotShowDialogWhenNoCurrentUrl() {
        RecordingHost host = new RecordingHost(null);
        AlertDialog dialog = promptFor(host);
        Assert.assertNull(dialog);
        Assert.assertTrue(host.showedNoUrlMessage);
    }

    @Test
    public void dialogTitleIsEditUrl() {
        AlertDialog dialog = promptFor(new RecordingHost("https://example.com/page"));
        Assert.assertEquals(context().getString(R.string.title_browser_edit_url),
            Shadows.shadowOf(dialog).getTitle());
    }

    @Test
    public void urlInputIsPrefilledWithCurrentUrl() {
        AlertDialog dialog = promptFor(new RecordingHost("https://example.com/page"));
        EditText input = dialog.findViewById(R.id.browser_edit_url_input);
        Assert.assertEquals("https://example.com/page", input.getText().toString());
    }

    @Test
    public void goButtonNavigatesToEditedUrlAndDismisses() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        EditText input = dialog.findViewById(R.id.browser_edit_url_input);
        input.setText("https://changed.example/x");
        button(dialog, R.id.browser_edit_url_go).performClick();
        Assert.assertEquals("https://changed.example/x", host.navigatedUrl);
        Assert.assertFalse(dialog.isShowing());
    }

    @Test
    public void copyButtonCopiesEditedUrlToClipboard() {
        AlertDialog dialog = promptFor(new RecordingHost("https://example.com/page"));
        button(dialog, R.id.browser_edit_url_copy).performClick();
        Assert.assertEquals("https://example.com/page", clipboardText());
    }

    @Test
    public void createSessionButtonDelegatesToHostAndDismisses() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        button(dialog, R.id.browser_edit_url_create_session).performClick();
        Assert.assertEquals("https://example.com/page", host.createdSessionUrl);
        Assert.assertFalse(dialog.isShowing());
    }

    @Test
    public void addBookmarkButtonDelegatesToHost() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        button(dialog, R.id.browser_edit_url_add_bookmark).performClick();
        Assert.assertEquals(1, host.addBookmarkCalls);
    }

    @Test
    public void bookmarksButtonDelegatesToHostAndDismisses() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        button(dialog, R.id.browser_edit_url_bookmarks).performClick();
        Assert.assertEquals(1, host.showBookmarksCalls);
        Assert.assertFalse(dialog.isShowing());
    }

    @Test
    public void openInChromeButtonDismissesDialog() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        button(dialog, R.id.browser_edit_url_open_in_chrome).performClick();
        Assert.assertFalse(dialog.isShowing());
    }

    @Test
    public void cancelButtonDismissesWithoutAction() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        AlertDialog dialog = promptFor(host);
        button(dialog, R.id.browser_edit_url_cancel).performClick();
        Assert.assertFalse(dialog.isShowing());
        Assert.assertNull(host.navigatedUrl);
        Assert.assertNull(host.createdSessionUrl);
    }

    @Test
    public void copyEditedUrlShowsNoUrlMessageForBlankInput() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        new BrowserUrlActions(context(), host).copyEditedUrlToClipboard("   ");
        Assert.assertTrue(host.showedNoUrlMessage);
    }

    @Test
    public void openEditedUrlInChromeShowsNoUrlMessageForBlankInput() {
        RecordingHost host = new RecordingHost("https://example.com/page");
        new BrowserUrlActions(context(), host).openEditedUrlInChrome(null);
        Assert.assertTrue(host.showedNoUrlMessage);
    }

    private String clipboardText() {
        ClipboardManager clipboardManager =
            (ClipboardManager) context().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null || clipboardManager.getPrimaryClip() == null) return null;
        return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
    }
}
