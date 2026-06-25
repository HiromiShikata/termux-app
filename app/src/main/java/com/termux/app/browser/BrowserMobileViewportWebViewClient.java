package com.termux.app.browser;

import android.graphics.Bitmap;
import android.webkit.WebView;

import androidx.annotation.NonNull;

public class BrowserMobileViewportWebViewClient extends BrowserHttpAuthWebViewClient {

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        injectMobileViewport(view);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        injectMobileViewport(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        injectMobileViewport(view);
    }

    protected void injectMobileViewport(@NonNull WebView view) {
        view.evaluateJavascript(BrowserMobileViewport.INJECTION_SCRIPT, null);
    }
}
