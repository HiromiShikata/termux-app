package com.termux.app.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class BrowserCoreWebChromeClientTest {

    private static final class RecordingHost implements BrowserCoreWebChromeClient.Host {

        Intent launchedIntent;
        BrowserPageLoadProgressState lastProgress;
        boolean titleReceived;
        String lastTitle;
        Bitmap lastIcon;
        boolean throwOnLaunch;

        @NonNull
        @Override
        public Context getDialogContext() {
            return RuntimeEnvironment.getApplication();
        }

        @Override
        public void launchFileChooser(@NonNull Intent intent) {
            if (throwOnLaunch) {
                throw new android.content.ActivityNotFoundException("no activity");
            }
            launchedIntent = intent;
        }

        @Override
        public void onProgressChanged(@NonNull BrowserPageLoadProgressState progressState) {
            lastProgress = progressState;
        }

        @Override
        public void onReceivedTitle(@Nullable String title) {
            titleReceived = true;
            lastTitle = title;
        }

        @Override
        public void onReceivedIcon(@NonNull Bitmap icon) {
            lastIcon = icon;
        }
    }

    private static final class RecordingFilePathCallback implements ValueCallback<Uri[]> {

        boolean received;
        Uri[] value;

        @Override
        public void onReceiveValue(Uri[] value) {
            received = true;
            this.value = value;
        }
    }

    private JsResult newJsResult() {
        return ReflectionHelpers.callConstructor(JsResult.class);
    }

    private JsPromptResult newJsPromptResult() {
        return ReflectionHelpers.callConstructor(JsPromptResult.class);
    }

    @Test
    public void onProgressChangedForwardsClampedProgressState() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        client.onProgressChanged(null, 40);
        Assert.assertNotNull(host.lastProgress);
        Assert.assertEquals(40, host.lastProgress.getProgress());
        Assert.assertTrue(host.lastProgress.isVisible());
    }

    @Test
    public void onReceivedTitleForwardsTitleToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        client.onReceivedTitle(null, "Page title");
        Assert.assertTrue(host.titleReceived);
        Assert.assertEquals("Page title", host.lastTitle);
    }

    @Test
    public void onReceivedIconForwardsNonNullIconToHost() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        client.onReceivedIcon(null, icon);
        Assert.assertSame(icon, host.lastIcon);
    }

    @Test
    public void onReceivedIconIgnoresNullIcon() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        client.onReceivedIcon(null, null);
        Assert.assertNull(host.lastIcon);
    }

    @Test
    public void onShowFileChooserReturnsFalseForNullCallback() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        Assert.assertFalse(client.onShowFileChooser(null, null, null));
        Assert.assertNull(host.launchedIntent);
    }

    @Test
    public void onShowFileChooserLaunchesGetContentIntentAndReturnsTrue() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        boolean handled = client.onShowFileChooser(null, callback, null);
        Assert.assertTrue(handled);
        Assert.assertNotNull(host.launchedIntent);
        Assert.assertEquals(Intent.ACTION_GET_CONTENT, host.launchedIntent.getAction());
        Assert.assertFalse(callback.received);
    }

    @Test
    public void onShowFileChooserHonorsAcceptTypesAndMultipleFlag() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        WebChromeClient.FileChooserParams params = new WebChromeClient.FileChooserParams() {
            @Override
            public int getMode() {
                return MODE_OPEN_MULTIPLE;
            }

            @Override
            public String[] getAcceptTypes() {
                return new String[]{"image/png"};
            }

            @Override
            public boolean isCaptureEnabled() {
                return false;
            }

            @Override
            public CharSequence getTitle() {
                return null;
            }

            @Override
            public String getFilenameHint() {
                return null;
            }

            @Override
            public Intent createIntent() {
                return new Intent();
            }
        };
        client.onShowFileChooser(null, callback, params);
        Assert.assertNotNull(host.launchedIntent);
        Assert.assertEquals("image/png", host.launchedIntent.getType());
        Assert.assertTrue(host.launchedIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
    }

    @Test
    public void deliverFileChooserResultPassesParsedUrisToCallback() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        client.onShowFileChooser(null, callback, null);
        Uri uri = Uri.parse("content://files/photo.png");
        Intent data = new Intent();
        data.setData(uri);
        client.deliverFileChooserResult(Activity.RESULT_OK, data);
        Assert.assertTrue(callback.received);
        Assert.assertNotNull(callback.value);
        Assert.assertEquals(1, callback.value.length);
        Assert.assertEquals(uri, callback.value[0]);
    }

    @Test
    public void deliverFileChooserResultReturnsNullOnCancel() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        client.onShowFileChooser(null, callback, null);
        client.deliverFileChooserResult(Activity.RESULT_CANCELED, null);
        Assert.assertTrue(callback.received);
        Assert.assertNull(callback.value);
    }

    @Test
    public void deliverFileChooserResultWithoutPendingCallbackDoesNothing() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        client.deliverFileChooserResult(Activity.RESULT_OK, new Intent());
    }

    @Test
    public void secondFileChooserCancelsTheFirstPendingCallback() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback first = new RecordingFilePathCallback();
        RecordingFilePathCallback second = new RecordingFilePathCallback();
        client.onShowFileChooser(null, first, null);
        client.onShowFileChooser(null, second, null);
        Assert.assertTrue(first.received);
        Assert.assertNull(first.value);
        Assert.assertFalse(second.received);
    }

    @Test
    public void onShowFileChooserReturnsFalseAndReleasesCallbackWhenLaunchFails() {
        RecordingHost host = new RecordingHost();
        host.throwOnLaunch = true;
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        boolean handled = client.onShowFileChooser(null, callback, null);
        Assert.assertFalse(handled);
        Assert.assertTrue(callback.received);
        Assert.assertNull(callback.value);
    }

    @Test
    public void cancelPendingFileChooserReturnsNullToPendingCallback() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        RecordingFilePathCallback callback = new RecordingFilePathCallback();
        client.onShowFileChooser(null, callback, null);
        client.cancelPendingFileChooser();
        Assert.assertTrue(callback.received);
        Assert.assertNull(callback.value);
    }

    @Test
    public void defaultHostDoesNotOpenNewTabsSoSessionBrowserBehaviorIsUnchanged() {
        RecordingHost host = new RecordingHost();
        Assert.assertFalse(host.openNewTabForUrl("https://example.com/new"));
    }

    @Test
    public void hostThatOpensNewTabsReceivesTheRequestedNewWindowUrl() {
        AtomicReference<String> openedUrlRef = new AtomicReference<>();
        BrowserCoreWebChromeClient.Host host = new BrowserCoreWebChromeClient.Host() {
            @NonNull
            @Override
            public Context getDialogContext() {
                return RuntimeEnvironment.getApplication();
            }

            @Override
            public void launchFileChooser(@NonNull Intent intent) {
            }

            @Override
            public void onProgressChanged(@NonNull BrowserPageLoadProgressState progressState) {
            }

            @Override
            public void onReceivedTitle(@Nullable String title) {
            }

            @Override
            public void onReceivedIcon(@NonNull Bitmap icon) {
            }

            @Override
            public boolean openNewTabForUrl(@NonNull String url) {
                openedUrlRef.set(url);
                return true;
            }
        };
        Assert.assertTrue(host.openNewTabForUrl("https://example.com/new"));
        Assert.assertEquals("https://example.com/new", openedUrlRef.get());
    }

    @Test
    public void onJsAlertShowsDialogAndReturnsTrue() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        boolean handled = client.onJsAlert(null, "https://example.com", "Alert message", newJsResult());
        Assert.assertTrue(handled);
        Assert.assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void onJsConfirmShowsDialogAndReturnsTrue() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        boolean handled = client.onJsConfirm(null, "https://example.com", "Confirm message", newJsResult());
        Assert.assertTrue(handled);
        Assert.assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void onJsPromptShowsDialogAndReturnsTrue() {
        RecordingHost host = new RecordingHost();
        BrowserCoreWebChromeClient client = new BrowserCoreWebChromeClient(host);
        boolean handled = client.onJsPrompt(null, "https://example.com", "Prompt message",
            "default", newJsPromptResult());
        Assert.assertTrue(handled);
        Assert.assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }
}
