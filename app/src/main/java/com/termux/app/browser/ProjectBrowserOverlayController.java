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

    private final View mOverviewActionsView;

    private final BrowserBulkOpenController mBulkOpenController;

    private boolean mVisible;

    private String mCurrentUrl;

    private final ProjectUrlRouter mRouter = new ProjectUrlRouter(this);

    public ProjectBrowserOverlayController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayContainer = activity.findViewById(R.id.project_browser_overlay);
        this.mWebView = activity.findViewById(R.id.project_browser_web_view);
        this.mHeaderUrlView = activity.findViewById(R.id.project_browser_header_url);
        this.mProgressBar = activity.findViewById(R.id.project_browser_progress_bar);
        this.mOverviewActionsView = activity.findViewById(R.id.project_browser_overview_actions);
        this.mBulkOpenController = new BrowserBulkOpenController(activity, mWebView);
        configureWebView();
        configureCloseButton();
        configureOverviewActions();
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
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
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

    private void configureOverviewActions() {
        mActivity.findViewById(R.id.project_browser_open_all_tasks_button)
            .setOnClickListener(view -> mBulkOpenController.openDisplayedTaskUrls(0));
        mActivity.findViewById(R.id.project_browser_open_first_ten_tasks_button)
            .setOnClickListener(view ->
                mBulkOpenController.openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    @Override
    public void openProjectUrl(@NonNull String url) {
        mHeaderUrlView.setText(url);
        mCurrentUrl = url;
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
        updateOverviewActionsVisibility();
    }

    public void hide() {
        mVisible = false;
        mProgressBar.setVisibility(View.GONE);
        mOverlayContainer.setVisibility(View.GONE);
        updateOverviewActionsVisibility();
    }

    private void updateOverviewActionsVisibility() {
        boolean showActions = mVisible && BrowserProjectOverviewPage.isOverviewUrl(mCurrentUrl);
        mOverviewActionsView.setVisibility(showActions ? View.VISIBLE : View.GONE);
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
