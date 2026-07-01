package com.termux.app.browser;

import android.graphics.Bitmap;
import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

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

        boolean onRenderProcessGone(@NonNull WebView view, boolean didCrash);

        void openInExternalBrowser(@NonNull String url);
    }

    private final Host mHost;

    private final BrowserWebViewCallbackGuard mCallbackGuard;

    public BrowserCoreWebViewClient(@NonNull Host host) {
        this(host, new BrowserWebViewCallbackGuard("BrowserCoreWebViewClient"));
    }

    BrowserCoreWebViewClient(@NonNull Host host, @NonNull BrowserWebViewCallbackGuard callbackGuard) {
        this.mHost = host;
        this.mCallbackGuard = callbackGuard;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return mCallbackGuard.runReturning("shouldOverrideUrlLoading", false, () -> {
            String url = request == null || request.getUrl() == null ? null : request.getUrl().toString();
            return routeToExternalBrowserIfRequired(url);
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return mCallbackGuard.runReturning("shouldOverrideUrlLoading", false,
            () -> routeToExternalBrowserIfRequired(url));
    }

    private boolean routeToExternalBrowserIfRequired(@Nullable String url) {
        if (url == null || !BrowserGoogleSignInRedirect.requiresExternalBrowser(url)) return false;
        mHost.openInExternalBrowser(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mCallbackGuard.run("onPageStarted", () -> {
            mHost.onPageStarted(view, url);
            injectViewport(view, url);
        });
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        mCallbackGuard.run("onPageCommitVisible", () -> {
            mHost.onPageCommitVisible(view, url);
            injectViewport(view, url);
        });
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mCallbackGuard.run("onPageFinished", () -> {
            boolean dismissed = mHost.onPageFinished(view, url);
            if (dismissed) return;
            injectViewport(view, url);
        });
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        mCallbackGuard.run("doUpdateVisitedHistory",
            () -> mHost.onVisitedHistoryUpdated(view, url, isReload));
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        mCallbackGuard.run("onReceivedError", () -> {
            if (!request.isForMainFrame()) return;
            mHost.onMainFrameError(view);
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        mCallbackGuard.run("onReceivedError", () -> mHost.onMainFrameError(view));
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.O)
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        return mCallbackGuard.runReturning("onRenderProcessGone", true, () -> {
            boolean didCrash = detail != null && detail.didCrash();
            boolean handled = mHost.onRenderProcessGone(view, didCrash);
            return handled || super.onRenderProcessGone(view, detail);
        });
    }

    private void injectViewport(@NonNull WebView view, @Nullable String url) {
        if (mHost.getViewMode().isDesktop()) {
            view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
        } else if (mHost.shouldInjectMobileViewport()) {
            view.evaluateJavascript(BrowserMobileViewport.INJECTION_SCRIPT, null);
        }
    }
}
