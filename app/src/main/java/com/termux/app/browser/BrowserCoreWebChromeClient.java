package com.termux.app.browser;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

public final class BrowserCoreWebChromeClient extends WebChromeClient {

    private static final String LOG_TAG = "BrowserCoreWebChromeClient";

    public interface Host {

        @NonNull
        Context getDialogContext();

        void launchFileChooser(@NonNull Intent intent);

        void onProgressChanged(@NonNull BrowserPageLoadProgressState progressState);

        void onReceivedTitle(@Nullable String title);

        void onReceivedIcon(@NonNull Bitmap icon);

        default boolean openNewTabForUrl(@NonNull String url) {
            return false;
        }
    }

    private final Host mHost;

    private ValueCallback<Uri[]> mPendingFileChooserCallback;

    public BrowserCoreWebChromeClient(@NonNull Host host) {
        this.mHost = host;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        mHost.onProgressChanged(BrowserPageLoadProgressState.forProgress(newProgress));
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        mHost.onReceivedTitle(title);
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        if (icon == null) return;
        mHost.onReceivedIcon(icon);
    }

    @Override
    public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        if (filePathCallback == null) {
            return false;
        }
        cancelPendingFileChooser();
        Intent intent = buildFileChooserIntent(fileChooserParams);
        try {
            mPendingFileChooserCallback = filePathCallback;
            mHost.launchFileChooser(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open file chooser", e);
            mPendingFileChooserCallback = null;
            filePathCallback.onReceiveValue(null);
            return false;
        }
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        if (resultMsg == null || view == null) return false;
        WebView hrefProbeWebView = new WebView(view.getContext());
        hrefProbeWebView.setWebViewClient(new BrowserNewWindowUrlProbeWebViewClient(mHost::openNewTabForUrl));
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(hrefProbeWebView);
        resultMsg.sendToTarget();
        return true;
    }

    @Override
    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        BrowserJsDialog.showAlert(mHost.getDialogContext(), message, result);
        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        BrowserJsDialog.showConfirm(mHost.getDialogContext(), message, result);
        return true;
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                              JsPromptResult result) {
        BrowserJsDialog.showPrompt(mHost.getDialogContext(), message, defaultValue, result);
        return true;
    }

    public void deliverFileChooserResult(int resultCode, @Nullable Intent data) {
        ValueCallback<Uri[]> callback = mPendingFileChooserCallback;
        mPendingFileChooserCallback = null;
        if (callback == null) {
            return;
        }
        callback.onReceiveValue(BrowserFileChooserResult.parse(resultCode, data));
    }

    public void cancelPendingFileChooser() {
        ValueCallback<Uri[]> callback = mPendingFileChooserCallback;
        mPendingFileChooserCallback = null;
        if (callback != null) {
            callback.onReceiveValue(null);
        }
    }

    @NonNull
    private static Intent buildFileChooserIntent(@Nullable FileChooserParams fileChooserParams) {
        if (fileChooserParams != null) {
            boolean allowMultiple = fileChooserParams.getMode()
                == FileChooserParams.MODE_OPEN_MULTIPLE;
            return BrowserFileChooserResult.buildIntent(allowMultiple, fileChooserParams.getAcceptTypes());
        }
        return BrowserFileChooserResult.buildIntent(false, null);
    }
}
