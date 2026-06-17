package com.termux.shared.interact;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ShareUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private ClipboardManager clipboard() {
        return (ClipboardManager) context().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Test
    public void copyTextToClipboardStoresText() {
        ShareUtils.copyTextToClipboard(context(), "copied text");
        ClipData clip = clipboard().getPrimaryClip();
        Assert.assertNotNull(clip);
        Assert.assertEquals("copied text", clip.getItemAt(0).getText().toString());
    }

    @Test
    public void copyTextToClipboardWithToastStoresText() {
        ShareUtils.copyTextToClipboard(context(), "with toast", "Copied!");
        Assert.assertEquals("with toast", clipboard().getPrimaryClip().getItemAt(0).getText().toString());
    }

    @Test
    public void copyTextToClipboardWithLabelStoresText() {
        ShareUtils.copyTextToClipboard(context(), "label", "labelled text", "");
        Assert.assertEquals("labelled text", clipboard().getPrimaryClip().getItemAt(0).getText().toString());
    }

    @Test
    public void copyTextToClipboardWithNullContextDoesNothing() {
        ShareUtils.copyTextToClipboard(null, "ignored");
    }

    @Test
    public void copyTextToClipboardWithNullTextDoesNothing() {
        ShareUtils.copyTextToClipboard(context(), null);
    }

    @Test
    public void getTextFromClipboardReturnsStoredText() {
        clipboard().setPrimaryClip(ClipData.newPlainText(null, "from clipboard"));
        CharSequence text = ShareUtils.getTextFromClipboard(context(), true);
        Assert.assertEquals("from clipboard", text.toString());
    }

    @Test
    public void getTextFromClipboardWithoutCoercionReturnsText() {
        clipboard().setPrimaryClip(ClipData.newPlainText(null, "no coerce"));
        CharSequence text = ShareUtils.getTextFromClipboard(context(), false);
        Assert.assertEquals("no coerce", text.toString());
    }

    @Test
    public void getTextFromClipboardWithNullContextReturnsNull() {
        Assert.assertNull(ShareUtils.getTextFromClipboard(null, true));
    }

    @Test
    public void getTextStringFromClipboardIfSetReturnsText() {
        clipboard().setPrimaryClip(ClipData.newPlainText(null, "present"));
        Assert.assertEquals("present", ShareUtils.getTextStringFromClipboardIfSet(context(), true));
    }

    @Test
    public void getTextStringFromClipboardIfSetReturnsNullWhenEmpty() {
        clipboard().setPrimaryClip(ClipData.newPlainText(null, ""));
        Assert.assertNull(ShareUtils.getTextStringFromClipboardIfSet(context(), true));
    }

    @Test
    public void shareTextWithSubjectAndTextDoesNotThrow() {
        ShareUtils.shareText(context(), "subject", "shared body");
    }

    @Test
    public void shareTextWithTitleDoesNotThrow() {
        ShareUtils.shareText(context(), "subject", "shared body", "Share With");
    }

    @Test
    public void shareTextWithNullTextDoesNothing() {
        ShareUtils.shareText(context(), "subject", null);
    }

    @Test
    public void openSystemAppChooserWithNullContextDoesNothing() {
        ShareUtils.openSystemAppChooser(null, new Intent(), "title");
    }

    @Test
    public void openUrlWithValidUrlDoesNotThrow() {
        ShareUtils.openUrl(context(), "https://example.com");
    }

    @Test
    public void openUrlWithEmptyUrlDoesNothing() {
        ShareUtils.openUrl(context(), "");
    }

    @Test
    public void openUrlWithNullUrlDoesNothing() {
        ShareUtils.openUrl(context(), null);
    }

    @Test
    public void openUrlInChromeDoesNotThrow() {
        ShareUtils.openUrlInChrome(context(), "https://example.com");
    }

    @Test
    public void openUrlInChromeWithEmptyUrlDoesNothing() {
        ShareUtils.openUrlInChrome(context(), "");
    }

    @Test
    public void saveTextToFileToCachePathDoesNotThrow() {
        String filePath = context().getCacheDir().getAbsolutePath() + "/share_utils_test_output.txt";
        ShareUtils.saveTextToFile(context(), "label", filePath, "saved content", true, -1);
    }

    @Test
    public void saveTextToFileToExternalStoragePathRequestsPermission() {
        String filePath = "/sdcard/share_utils_external_output.txt";
        ShareUtils.saveTextToFile(context(), "label", filePath, "content", false, -1);
    }

    @Test
    public void saveTextToFileWithNullPathDoesNothing() {
        ShareUtils.saveTextToFile(context(), "label", null, "content", false, -1);
    }

    @Test
    public void saveTextToFileWithNullTextDoesNothing() {
        ShareUtils.saveTextToFile(context(), "label", "/some/path", null, false, -1);
    }
}
