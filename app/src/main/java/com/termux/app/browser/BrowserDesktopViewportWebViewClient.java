package com.termux.app.browser;

import android.graphics.Bitmap;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

public class BrowserDesktopViewportWebViewClient extends WebViewClient {

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        BrowserHttpAuthDialog.show(view.getContext(), handler, host, realm);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        injectDesktopViewport(view);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        injectDesktopViewport(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        injectDesktopViewport(view);
    }

    protected void injectDesktopViewport(@NonNull WebView view) {
        view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
    }
}
