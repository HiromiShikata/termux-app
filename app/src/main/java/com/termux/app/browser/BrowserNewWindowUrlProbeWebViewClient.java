package com.termux.app.browser;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserNewWindowUrlProbeWebViewClient extends WebViewClient {

    public interface NewWindowUrlListener {
        boolean openNewTabForUrl(@NonNull String url);
    }

    private final NewWindowUrlListener mListener;

    private boolean mDelivered;

    public BrowserNewWindowUrlProbeWebViewClient(@NonNull NewWindowUrlListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return deliver(view, request == null || request.getUrl() == null ? null : request.getUrl().toString());
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return deliver(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        deliver(view, url);
    }

    private boolean deliver(@Nullable WebView probeWebView, @Nullable String url) {
        if (mDelivered) return true;
        mDelivered = true;
        if (probeWebView != null) {
            probeWebView.stopLoading();
            probeWebView.destroy();
        }
        if (url == null || url.isEmpty() || "about:blank".equals(url)) return true;
        mListener.openNewTabForUrl(url);
        return true;
    }
}
