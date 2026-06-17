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
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.TermuxActivity;

public final class ProjectBrowserOverlayController implements ProjectUrlOpener {

    private final TermuxActivity mActivity;

    private final View mOverlayContainer;

    private final WebView mWebView;

    private final TextView mHeaderUrlView;

    private final ProgressBar mProgressBar;

    private final View mWebViewCover;

    private final View mOverviewActionsView;

    private final BrowserBulkOpenController mBulkOpenController;

    private boolean mVisible;

    private String mCurrentUrl;

    private String mLoadedUrl;

    private final ProjectUrlRouter mRouter = new ProjectUrlRouter(this);

    public ProjectBrowserOverlayController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayContainer = activity.findViewById(R.id.project_browser_overlay);
        this.mWebView = activity.findViewById(R.id.project_browser_web_view);
        this.mHeaderUrlView = activity.findViewById(R.id.project_browser_header_url);
        this.mProgressBar = activity.findViewById(R.id.project_browser_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.project_browser_web_view_cover);
        this.mOverviewActionsView = activity.findViewById(R.id.project_browser_overview_actions);
        this.mBulkOpenController = new BrowserBulkOpenController(activity, mWebView);
        configureWebView();
        configureLinkContextMenu();
        configureCloseButton();
        configureOverviewActions();
    }

    private void configureLinkContextMenu() {
        new BrowserLinkContextMenuController(mActivity, mWebView, new BrowserLinkContextMenuController.Actions() {
            @Override
            public void openLinkInBrowser(@NonNull String linkUrl) {
                openProjectUrl(linkUrl);
            }

            @Override
            public void createSessionForLink(@NonNull String linkUrl) {
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(linkUrl);
            }
        }).attach();
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

        mWebView.setWebViewClient(new BrowserDesktopViewportWebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (BrowserPageTransition.requiresCoverWhileLoading(mLoadedUrl, url, mVisible)) {
                    mWebViewCover.setVisibility(View.VISIBLE);
                }
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                CookieManager.getInstance().flush();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                BrowserPageLoadProgressState progressState =
                    BrowserPageLoadProgressState.forProgress(newProgress);
                mProgressBar.setProgress(progressState.getProgress());
                mProgressBar.setVisibility(progressState.isVisible() ? View.VISIBLE : View.GONE);
                if (!progressState.isVisible()) {
                    mWebViewCover.setVisibility(View.GONE);
                }
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
        mWebViewCover.setVisibility(View.GONE);
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

    @Nullable
    public String getCurrentUrl() {
        return mCurrentUrl;
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
