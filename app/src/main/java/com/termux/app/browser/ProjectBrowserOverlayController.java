package com.termux.app.browser;

import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;

public final class ProjectBrowserOverlayController implements ProjectUrlOpener {

    private final TermuxActivity mActivity;

    private final View mOverlayContainer;

    private final WebView mWebView;

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final TextView mHeaderUrlView;

    private final ProgressBar mProgressBar;

    private final View mWebViewCover;

    private final View mOverviewActionsView;

    private final BrowserBulkOpenController mBulkOpenController;

    private boolean mVisible;

    private String mCurrentUrl;

    private String mLoadedUrl;

    private BrowserViewMode mViewMode = BrowserViewMode.MOBILE;

    private String mDefaultUserAgent;

    private final ProjectUrlRouter mRouter = new ProjectUrlRouter(this);

    public ProjectBrowserOverlayController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayContainer = activity.findViewById(R.id.project_browser_overlay);
        this.mWebView = activity.findViewById(R.id.project_browser_web_view);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.project_browser_swipe_refresh);
        this.mHeaderUrlView = activity.findViewById(R.id.project_browser_header_url);
        this.mProgressBar = activity.findViewById(R.id.project_browser_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.project_browser_web_view_cover);
        this.mOverviewActionsView = activity.findViewById(R.id.project_browser_overview_actions);
        this.mBulkOpenController = new BrowserBulkOpenController(activity);
        configureWebView();
        configureLinkContextMenu();
        configureHeaderUrlMenu();
        configureCloseButton();
        configureOverviewActions();
    }

    private void configureHeaderUrlMenu() {
        BrowserHeaderUrlMenuController menuController =
            new BrowserHeaderUrlMenuController(mActivity, url ->
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(url));
        mHeaderUrlView.setOnClickListener(view -> menuController.showHeaderUrlMenu(mCurrentUrl));
    }

    private void configureLinkContextMenu() {
        new BrowserLinkContextMenuController(mActivity, mWebView, new BrowserLinkContextMenuController.Actions() {
            @Override
            public void openLinkInBrowser(@NonNull String linkUrl) {
                openProjectUrl(linkUrl, mViewMode);
            }

            @Override
            public void createSessionForLink(@NonNull String linkUrl) {
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(linkUrl);
            }
        }).attach();
    }

    private void configureWebView() {
        applyViewModeConfiguration(mViewMode);

        mSwipeRefreshLayout.setOnRefreshListener(mWebView::reload);
        mSwipeRefreshLayout.setOnChildScrollUpCallback((parent, child) ->
            BrowserPullToRefreshGate.canWebViewScrollUp(mWebView.getScrollY()));

        mWebView.setWebViewClient(new BrowserMobileViewportWebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectDesktopViewportIfNeeded(view);
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
                injectDesktopViewportIfNeeded(view);
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectDesktopViewportIfNeeded(view);
                if (mVisible && "about:blank".equals(url)) {
                    hide();
                    return;
                }
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (!request.isForMainFrame()) return;
                onMainFrameError();
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                onMainFrameError();
            }

            @Override
            protected void injectMobileViewport(@NonNull WebView view) {
                if (mViewMode.isDesktop()) return;
                super.injectMobileViewport(view);
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
            .setOnClickListener(view -> mBulkOpenController.openDisplayedTaskUrls(mWebView, 0));
        mActivity.findViewById(R.id.project_browser_open_first_ten_tasks_button)
            .setOnClickListener(view ->
                mBulkOpenController.openDisplayedTaskUrls(mWebView, BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    @Override
    public void openProjectUrl(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        applyViewModeConfiguration(viewMode);
        if (BrowserPageTransition.requiresCoverWhileLoading(mLoadedUrl, url, true)) {
            mWebViewCover.setVisibility(View.VISIBLE);
        }
        mHeaderUrlView.setText(url);
        mCurrentUrl = url;
        mWebView.loadUrl(url);
        show();
    }

    private void applyViewModeConfiguration(@NonNull BrowserViewMode viewMode) {
        mViewMode = viewMode;
        WebSettings settings = mWebView.getSettings();
        if (mDefaultUserAgent == null) {
            mDefaultUserAgent = BrowserUserAgent.normalizeDefault(settings.getUserAgentString());
        }
        if (viewMode.isDesktop()) {
            BrowserDesktopWebViewConfigurator.apply(settings);
        } else {
            BrowserMobileWebViewConfigurator.apply(settings);
            settings.setUserAgentString(mDefaultUserAgent);
        }
    }

    private void injectDesktopViewportIfNeeded(@NonNull WebView view) {
        if (mViewMode.isDesktop()) {
            view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
        }
    }

    public void route(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        mRouter.route(url, viewMode);
    }

    private void onMainFrameError() {
        mWebViewCover.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void show() {
        mVisible = true;
        mOverlayContainer.setVisibility(View.VISIBLE);
        mWebView.requestFocus();
        updateOverviewActionsVisibility();
    }

    public void hide() {
        mVisible = false;
        mProgressBar.setVisibility(View.GONE);
        mWebViewCover.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mOverlayContainer.setVisibility(View.GONE);
        resetWebViewToBlank();
        updateOverviewActionsVisibility();
    }

    private void resetWebViewToBlank() {
        mCurrentUrl = null;
        mLoadedUrl = null;
        mHeaderUrlView.setText("");
        mWebView.loadUrl("about:blank");
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
            WebBackForwardList backForwardList = mWebView.copyBackForwardList();
            int previousIndex = backForwardList.getCurrentIndex() - 1;
            if (previousIndex >= 0 && "about:blank".equals(backForwardList.getItemAtIndex(previousIndex).getUrl())) {
                hide();
            } else {
                mWebView.goBack();
            }
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
