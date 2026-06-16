package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;

public final class ProjectBrowserOverlayController implements ProjectUrlOpener {

    private final TermuxActivity mActivity;

    private final View mOverlayContainer;

    private final WebView mWebView;

    private final TextView mHeaderUrlView;

    private final ProgressBar mProgressBar;

    private boolean mVisible;

    private final ProjectUrlRouter mRouter = new ProjectUrlRouter(this);

    public ProjectBrowserOverlayController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayContainer = activity.findViewById(R.id.project_browser_overlay);
        this.mWebView = activity.findViewById(R.id.project_browser_web_view);
        this.mHeaderUrlView = activity.findViewById(R.id.project_browser_header_url);
        this.mProgressBar = activity.findViewById(R.id.project_browser_progress_bar);
        configureWebView();
        configureCloseButton();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUserAgentString(BrowserUserAgent.DESKTOP_USER_AGENT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        BrowserWebAuthentication.apply(settings);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mHeaderUrlView.setText(url);
                view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                mHeaderUrlView.setText(url);
                CookieManager.getInstance().flush();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void configureCloseButton() {
        View closeButton = mActivity.findViewById(R.id.project_browser_close_button);
        closeButton.setOnClickListener(view -> hide());
    }

    @Override
    public void openProjectUrl(@NonNull String url) {
        mHeaderUrlView.setText(url);
        mWebView.loadUrl(url);
        show();
    }

    public void route(@NonNull String url) {
        mRouter.route(url);
    }

    private void show() {
        mVisible = true;
        mOverlayContainer.setVisibility(View.VISIBLE);
        mOverlayContainer.bringToFront();
    }

    public void hide() {
        mVisible = false;
        mProgressBar.setVisibility(View.GONE);
        mOverlayContainer.setVisibility(View.GONE);
    }

    public boolean isVisible() {
        return mVisible;
    }

    public boolean onBackPressed() {
        if (!mVisible) return false;
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        hide();
        return true;
    }

    public void onActivityDestroy() {
        mWebView.stopLoading();
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("about:blank");
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}
