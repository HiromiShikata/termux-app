package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class ProjectBrowserOverlayController implements ProjectUrlOpener {

    private static final String LOG_TAG = "ProjectBrowserOverlayController";

    public static final int REQUEST_PROJECT_BROWSER_FILE_CHOOSER = 3001;

    private final TermuxActivity mActivity;

    private final View mOverlayContainer;

    private final WebView mWebView;

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final TextView mHeaderUrlView;

    private final ProgressBar mProgressBar;

    private final View mWebViewCover;

    private final View mOverviewActionsView;

    private final BrowserBulkOpenController mBulkOpenController;

    private final BrowserBookmarkSerializer mBookmarkSerializer = new BrowserBookmarkSerializer();

    private BrowserUrlActions mUrlActions;

    private boolean mVisible;

    private String mCurrentUrl;

    private String mLoadedUrl;

    private BrowserViewMode mViewMode = BrowserViewMode.MOBILE;

    private String mDefaultUserAgent;

    private final ProjectUrlRouter mRouter = new ProjectUrlRouter(this);

    private final BrowserDownloadController mDownloadController = new BrowserDownloadController(
        new BrowserDownloadController.Host() {
            @NonNull
            @Override
            public Context getDownloadContext() {
                return mActivity;
            }

            @Override
            public boolean isActivityVisible() {
                return mActivity.isVisible();
            }

            @Override
            public void onDownloadStarted(@NonNull String fileName) {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_started, fileName), false);
            }

            @Override
            public void onDownloadComplete() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_complete), false);
            }

            @Override
            public void onDownloadFailed() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
            }
        });

    private BrowserCoreWebChromeClient mWebChromeClient;

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
        mUrlActions = new BrowserUrlActions(mActivity, new BrowserUrlActions.Host() {
            @Override
            public String currentPageUrl() {
                return BrowserHeaderUrlMenuEligibility.canShowMenuFor(mCurrentUrl) ? mCurrentUrl : null;
            }

            @Override
            public void showNoCurrentUrlMessage() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            }

            @Override
            public void navigateToUrl(@NonNull String editedUrl) {
                openProjectUrl(BrowserUrlInput.normalize(editedUrl), mViewMode);
            }

            @Override
            public void createSessionForUrl(@NonNull String editedUrl) {
                String sessionName = BrowserEditedUrl.sessionNameFor(editedUrl);
                if (sessionName == null) {
                    mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
                    return;
                }
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(sessionName);
            }

            @Override
            public void addCurrentPageBookmark() {
                addCurrentPageBookmarkToStore();
            }

            @Override
            public void showBookmarksList() {
                showBookmarksListDialog();
            }
        });
        mHeaderUrlView.setOnClickListener(view -> {
            if (!BrowserHeaderUrlMenuEligibility.canShowMenuFor(mCurrentUrl)) return;
            mUrlActions.promptEditCurrentPageUrl();
        });
    }

    private void addCurrentPageBookmarkToStore() {
        if (!BrowserHeaderUrlMenuEligibility.canShowMenuFor(mCurrentUrl)) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        String title = mWebView.getTitle();
        BrowserBookmark bookmark = new BrowserBookmark(mCurrentUrl, title == null ? mCurrentUrl : title);
        saveBookmarks(loadBookmarks().added(bookmark));
        mActivity.showToast(mActivity.getString(R.string.msg_browser_bookmarked), false);
    }

    private void showBookmarksListDialog() {
        List<BrowserBookmark> bookmarks = loadBookmarks().getBookmarks();
        if (bookmarks.isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_bookmarks), false);
            return;
        }
        List<String> labels = new ArrayList<>();
        for (BrowserBookmark bookmark : bookmarks) {
            labels.add(bookmark.getTitle() + "\n" + bookmark.getUrl());
        }
        ListView listView = new ListView(mActivity);
        listView.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1, labels));
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_browser_bookmarks)
            .setView(listView)
            .create();
        dialog.setCanceledOnTouchOutside(true);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            openProjectUrl(bookmarks.get(position).getUrl(), mViewMode);
            dialog.dismiss();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            promptDeleteBookmark(bookmarks.get(position));
            return true;
        });
        dialog.show();
    }

    private void promptDeleteBookmark(@NonNull BrowserBookmark bookmark) {
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_browser_bookmark_delete)
            .setMessage(bookmark.getUrl())
            .setPositiveButton(R.string.action_browser_bookmark_delete, (dialog, which) -> {
                saveBookmarks(loadBookmarks().removed(bookmark.getUrl()));
                showBookmarksListDialog();
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> showBookmarksListDialog()));
    }

    @NonNull
    private BrowserBookmarkCollection loadBookmarks() {
        try {
            return new BrowserBookmarkCollection(
                mBookmarkSerializer.deserialize(mActivity.getPreferences().getBrowserBookmarks()));
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to deserialize browser bookmarks, clearing the store", e);
            mActivity.getPreferences().setBrowserBookmarks(null);
            return new BrowserBookmarkCollection(new ArrayList<>());
        }
    }

    private void saveBookmarks(@NonNull BrowserBookmarkCollection bookmarks) {
        try {
            mActivity.getPreferences().setBrowserBookmarks(mBookmarkSerializer.serialize(bookmarks.getBookmarks()));
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize browser bookmarks", e);
        }
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

        mWebView.setWebViewClient(new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host() {
            @NonNull
            @Override
            public BrowserViewMode getViewMode() {
                return mViewMode;
            }

            @Override
            public boolean shouldInjectMobileViewport() {
                return true;
            }

            @Override
            public void onPageStarted(@NonNull WebView view, @Nullable String url) {
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
            public void onPageCommitVisible(@NonNull WebView view, @Nullable String url) {
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
            }

            @Override
            public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
                if (mVisible && "about:blank".equals(url)) {
                    hide();
                    return true;
                }
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                CookieManager.getInstance().flush();
                return false;
            }

            @Override
            public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
            }

            @Override
            public void onMainFrameError(@NonNull WebView view) {
                handleMainFrameError();
            }
        }));

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            mDownloadController.enqueueDownload(url, userAgent, contentDisposition, mimetype));

        mWebChromeClient = new BrowserCoreWebChromeClient(new BrowserCoreWebChromeClient.Host() {
            @NonNull
            @Override
            public Context getDialogContext() {
                return mActivity;
            }

            @Override
            public void launchFileChooser(@NonNull Intent intent) {
                mActivity.startActivityForResult(intent, REQUEST_PROJECT_BROWSER_FILE_CHOOSER);
            }

            @Override
            public void onProgressChanged(@NonNull BrowserPageLoadProgressState progressState) {
                mProgressBar.setProgress(progressState.getProgress());
                mProgressBar.setVisibility(progressState.isVisible() ? View.VISIBLE : View.GONE);
                if (!progressState.isVisible()) {
                    mWebViewCover.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(@Nullable String title) {
            }

            @Override
            public void onReceivedIcon(@NonNull Bitmap icon) {
            }
        });
        mWebView.setWebChromeClient(mWebChromeClient);
    }

    public void deliverFileChooserResult(int resultCode, @Nullable Intent data) {
        if (mWebChromeClient != null) {
            mWebChromeClient.deliverFileChooserResult(resultCode, data);
        }
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
        BrowserWebViewConfigurator.apply(settings, viewMode, mDefaultUserAgent);
    }

    public void route(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        mRouter.route(url, viewMode);
    }

    private void handleMainFrameError() {
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
        if (mWebChromeClient != null) {
            mWebChromeClient.cancelPendingFileChooser();
        }
        mDownloadController.unregisterDownloadCompleteReceiver();
        mWebView.stopLoading();
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("about:blank");
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}
