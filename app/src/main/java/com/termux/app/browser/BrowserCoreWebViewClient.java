package com.termux.app.browser;

import android.graphics.Bitmap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserCoreWebViewClient extends BrowserHttpAuthWebViewClient {

    public interface Host {

        @NonNull
        BrowserViewMode getViewMode();

        boolean shouldInjectMobileViewport();

        void onPageStarted(@NonNull WebView view, @Nullable String url);

        void onPageCommitVisible(@NonNull WebView view, @Nullable String url);

        boolean onPageFinished(@NonNull WebView view, @Nullable String url);

        void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload);

        void onMainFrameError(@NonNull WebView view);
    }

    private final Host mHost;

    public BrowserCoreWebViewClient(@NonNull Host host) {
        this.mHost = host;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mHost.onPageStarted(view, url);
        injectViewport(view);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        mHost.onPageCommitVisible(view, url);
        injectViewport(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        boolean dismissed = mHost.onPageFinished(view, url);
        if (dismissed) return;
        injectViewport(view);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        mHost.onVisitedHistoryUpdated(view, url, isReload);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        if (!request.isForMainFrame()) return;
        mHost.onMainFrameError(view);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        mHost.onMainFrameError(view);
    }

    private void injectViewport(@NonNull WebView view) {
        if (mHost.getViewMode().isDesktop()) {
            view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
        } else if (mHost.shouldInjectMobileViewport()) {
            view.evaluateJavascript(BrowserMobileViewport.INJECTION_SCRIPT, null);
        }
    }
}
