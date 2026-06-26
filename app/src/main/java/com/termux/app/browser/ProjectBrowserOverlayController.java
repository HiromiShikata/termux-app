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
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
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
import com.termux.shared.termux.interact.TextInputDialogUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectBrowserOverlayController implements ProjectUrlOpener, BrowserTabSelectionListener {

    private static final String LOG_TAG = "ProjectBrowserOverlayController";

    public static final int REQUEST_PROJECT_BROWSER_FILE_CHOOSER = 3001;

    private static final String PROJECT_BROWSER_SESSION_HANDLE = "project-browser-overlay";

    private final TermuxActivity mActivity;

    private final View mOverlayContainer;

    private final FrameLayout mWebViewContainer;

    private final BrowserTabWebViewHost mWebViewHost;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final TextView mHeaderUrlView;

    private final ProgressBar mProgressBar;

    private final View mWebViewCover;

    private final View mOverviewActionsView;

    private final View mFooterOverviewIconView;

    private final View mFooterTdpmConsoleIconView;

    private final View mFooterNewIssueIconView;

    private final BrowserTabFaviconStripController mTabFaviconStripController;

    private final BrowserBulkOpenController mBulkOpenController;

    private final BrowserBookmarkSerializer mBookmarkSerializer = new BrowserBookmarkSerializer();

    private BrowserUrlActions mUrlActions;

    private String mProjectOverviewUrl;

    private String mProjectTdpmConsoleUrl;

    private String mProjectNewIssueUrl;

    private boolean mVisible;

    private String mCurrentUrl;

    private String mLoadedUrl;

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

    private final Map<BrowserTab, BrowserCoreWebChromeClient> mWebChromeClientByTab = new IdentityHashMap<>();

    private BrowserCoreWebChromeClient mWebChromeClient;

    public ProjectBrowserOverlayController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mOverlayContainer = activity.findViewById(R.id.project_browser_overlay);
        this.mWebViewContainer = activity.findViewById(R.id.project_browser_web_view_container);
        this.mWebViewHost = new BrowserTabWebViewHost(mWebViewContainer, this::createWebViewForTab);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.project_browser_swipe_refresh);
        this.mHeaderUrlView = activity.findViewById(R.id.project_browser_header_url);
        this.mProgressBar = activity.findViewById(R.id.project_browser_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.project_browser_web_view_cover);
        this.mOverviewActionsView = activity.findViewById(R.id.project_browser_overview_actions);
        this.mFooterOverviewIconView = activity.findViewById(R.id.project_browser_footer_overview_icon);
        this.mFooterTdpmConsoleIconView = activity.findViewById(R.id.project_browser_footer_tdpm_console_icon);
        this.mFooterNewIssueIconView = activity.findViewById(R.id.project_browser_footer_new_issue_icon);
        HorizontalScrollView tabStripScroll = activity.findViewById(R.id.project_browser_tab_strip_scroll);
        LinearLayout tabStripContainer = activity.findViewById(R.id.project_browser_tab_strip_container);
        this.mTabFaviconStripController = new BrowserTabFaviconStripController(tabStripScroll, tabStripContainer, this);
        this.mBulkOpenController = new BrowserBulkOpenController(activity);
        configureSwipeRefresh();
        configureHeaderUrlMenu();
        configureCloseButton();
        configureOverviewActions();
        configureFooterActions();
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
                openProjectUrl(BrowserUrlInput.normalize(editedUrl), activeViewMode());
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
        WebView activeWebView = mWebViewHost.getDisplayedWebView();
        String title = activeWebView == null ? null : activeWebView.getTitle();
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
            openProjectUrl(bookmarks.get(position).getUrl(), activeViewMode());
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

    private void configureSwipeRefresh() {
        mSwipeRefreshLayout.setOnRefreshListener(this::reloadDisplayedWebView);
        mSwipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
            WebView displayedWebView = mWebViewHost.getDisplayedWebView();
            return displayedWebView != null
                && BrowserPullToRefreshGate.canWebViewScrollUp(displayedWebView.getScrollY());
        });
    }

    private void reloadDisplayedWebView() {
        WebView displayedWebView = mWebViewHost.getDisplayedWebView();
        if (displayedWebView != null) displayedWebView.reload();
    }

    @NonNull
    private WebView createWebViewForTab(@NonNull BrowserTab tab) {
        WebView webView = new WebView(mActivity);
        WebSettings settings = webView.getSettings();
        if (mDefaultUserAgent == null) {
            mDefaultUserAgent = BrowserUserAgent.normalizeDefault(settings.getUserAgentString());
        }
        BrowserWebViewConfigurator.apply(settings, tab.getViewMode(), mDefaultUserAgent);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host() {
            @NonNull
            @Override
            public BrowserViewMode getViewMode() {
                return tab.getViewMode();
            }

            @Override
            public boolean shouldInjectMobileViewport() {
                return true;
            }

            @Override
            public void onPageStarted(@NonNull WebView view, @Nullable String url) {
                tab.setUrl(url);
                if (!isDisplayedTab(tab)) return;
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
                if (!isDisplayedTab(tab)) return;
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
            }

            @Override
            public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
                if (isDisplayedTab(tab) && mVisible && "about:blank".equals(url)) {
                    hide();
                    return true;
                }
                tab.setUrl(url);
                tab.setTitle(view.getTitle());
                CookieManager.getInstance().flush();
                if (!isDisplayedTab(tab)) {
                    notifyTabsUpdated();
                    return false;
                }
                mLoadedUrl = url;
                mWebViewCover.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);
                mHeaderUrlView.setText(url);
                mCurrentUrl = url;
                updateOverviewActionsVisibility();
                notifyTabsUpdated();
                return false;
            }

            @Override
            public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
            }

            @Override
            public void onMainFrameError(@NonNull WebView view) {
                if (isDisplayedTab(tab)) handleMainFrameError();
            }
        }));

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            mDownloadController.enqueueDownload(url, userAgent, contentDisposition, mimetype));

        BrowserCoreWebChromeClient chromeClient = new BrowserCoreWebChromeClient(new BrowserCoreWebChromeClient.Host() {
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
                if (!isDisplayedTab(tab)) return;
                mProgressBar.setProgress(progressState.getProgress());
                mProgressBar.setVisibility(progressState.isVisible() ? View.VISIBLE : View.GONE);
                if (!progressState.isVisible()) {
                    mWebViewCover.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(@Nullable String title) {
                tab.setTitle(title);
                notifyTabsUpdated();
            }

            @Override
            public void onReceivedIcon(@NonNull Bitmap icon) {
                tab.setFavicon(icon);
                notifyTabsUpdated();
            }

            @Override
            public boolean openNewTabForUrl(@NonNull String url) {
                openProjectUrlInNewTab(url, tab.getViewMode());
                return true;
            }
        });
        mWebChromeClient = chromeClient;
        mWebChromeClientByTab.put(tab, chromeClient);
        webView.setWebChromeClient(mWebChromeClient);

        new BrowserLinkContextMenuController(mActivity, webView, new BrowserLinkContextMenuController.Actions() {
            @Override
            public void openLinkInBrowser(@NonNull String linkUrl) {
                openProjectUrlInNewTab(linkUrl, tab.getViewMode());
            }

            @Override
            public void createSessionForLink(@NonNull String linkUrl) {
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(linkUrl);
            }
        }).attach();

        return webView;
    }

    private boolean isDisplayedTab(@NonNull BrowserTab tab) {
        return mWebViewHost.getDisplayedTab() == tab;
    }

    public void deliverFileChooserResult(int resultCode, @Nullable Intent data) {
        BrowserCoreWebChromeClient displayedChromeClient = displayedWebChromeClient();
        if (displayedChromeClient != null) {
            displayedChromeClient.deliverFileChooserResult(resultCode, data);
        }
    }

    @Nullable
    private BrowserCoreWebChromeClient displayedWebChromeClient() {
        BrowserTab displayedTab = mWebViewHost.getDisplayedTab();
        return displayedTab == null ? mWebChromeClient : mWebChromeClientByTab.get(displayedTab);
    }

    private void configureCloseButton() {
        View closeButton = mActivity.findViewById(R.id.project_browser_close_button);
        closeButton.setOnClickListener(view -> hide());
    }

    private void configureOverviewActions() {
        mActivity.findViewById(R.id.project_browser_open_all_tasks_button)
            .setOnClickListener(view -> openDisplayedTaskUrls(0));
        mActivity.findViewById(R.id.project_browser_open_first_ten_tasks_button)
            .setOnClickListener(view -> openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    private void openDisplayedTaskUrls(int limit) {
        WebView displayedWebView = mWebViewHost.getDisplayedWebView();
        if (displayedWebView == null) return;
        mBulkOpenController.openDisplayedTaskUrls(displayedWebView, limit);
    }

    private void configureFooterActions() {
        mFooterOverviewIconView.setOnClickListener(view ->
            route(mProjectOverviewUrl, BrowserViewMode.DESKTOP));
        mFooterTdpmConsoleIconView.setOnClickListener(view ->
            route(mProjectTdpmConsoleUrl, BrowserViewMode.MOBILE));
        mFooterNewIssueIconView.setOnClickListener(view ->
            route(mProjectNewIssueUrl, BrowserViewMode.DESKTOP));
        updateFooterActionsVisibility();
    }

    public void setProjectContext(@Nullable String overviewUrl, @Nullable String tdpmConsoleUrl,
                                  @Nullable String newIssueUrl) {
        mProjectOverviewUrl = overviewUrl;
        mProjectTdpmConsoleUrl = tdpmConsoleUrl;
        mProjectNewIssueUrl = newIssueUrl;
        updateFooterActionsVisibility();
    }

    private void updateFooterActionsVisibility() {
        applyFooterIconVisibility(mFooterOverviewIconView, mProjectOverviewUrl);
        applyFooterIconVisibility(mFooterTdpmConsoleIconView, mProjectTdpmConsoleUrl);
        applyFooterIconVisibility(mFooterNewIssueIconView, mProjectNewIssueUrl);
    }

    private static void applyFooterIconVisibility(@NonNull View iconView, @Nullable String url) {
        iconView.setVisibility(url == null || url.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void openProjectUrl(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        BrowserTab tab = mTabManager.attachOrActivateTab(PROJECT_BROWSER_SESSION_HANDLE, url);
        tab.setViewMode(viewMode);
        displayTab(tab, url);
        show();
    }

    public void openProjectUrlInNewTab(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        String normalizedUrl = BrowserUrlInput.normalize(url);
        BrowserTab tab = mTabManager.addTab(PROJECT_BROWSER_SESSION_HANDLE, normalizedUrl);
        tab.setViewMode(viewMode);
        displayTab(tab, normalizedUrl);
        show();
    }

    private void displayTab(@NonNull BrowserTab tab, @NonNull String url) {
        mTabManager.setActiveTab(tab);
        if (BrowserPageTransition.requiresCoverWhileLoading(mLoadedUrl, url, true)) {
            mWebViewCover.setVisibility(View.VISIBLE);
        }
        mHeaderUrlView.setText(url);
        mCurrentUrl = url;
        WebView webView = mWebViewHost.showTab(tab);
        BrowserWebViewConfigurator.apply(webView.getSettings(), tab.getViewMode(), mDefaultUserAgent);
        if (!url.equals(webView.getUrl())) {
            webView.loadUrl(url);
        }
        notifyTabsUpdated();
    }

    public void route(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        mRouter.route(url, viewMode);
    }

    @Override
    public void openTab(@NonNull BrowserTab tab) {
        mTabManager.setActiveTab(tab);
        WebView webView = mWebViewHost.showTab(tab);
        String tabUrl = tab.getUrl();
        mCurrentUrl = tabUrl;
        mLoadedUrl = webView.getUrl();
        mHeaderUrlView.setText(tabUrl);
        updateOverviewActionsVisibility();
        notifyTabsUpdated();
        show();
    }

    @Override
    public void closeTab(@NonNull BrowserTab tab) {
        BrowserCoreWebChromeClient chromeClient = mWebChromeClientByTab.remove(tab);
        if (chromeClient != null) chromeClient.cancelPendingFileChooser();
        mTabManager.removeTab(tab);
        mWebViewHost.removeTab(tab);
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            notifyTabsUpdated();
            hide();
            return;
        }
        openTab(activeTab);
    }

    @Override
    public void promptNewTab() {
        TextInputDialogUtils.textInput(mActivity, R.string.title_browser_open_url, null,
            R.string.action_browser_open_url_confirm, text ->
                openProjectUrlInNewTab(BrowserUrlInput.normalize(text), activeViewMode()),
            -1, null, android.R.string.cancel, null, null);
    }

    @Nullable
    @Override
    public BrowserTab getActiveTab() {
        return mTabManager.getActiveTab(PROJECT_BROWSER_SESSION_HANDLE);
    }

    @NonNull
    private BrowserViewMode activeViewMode() {
        BrowserTab activeTab = getActiveTab();
        return activeTab == null ? BrowserViewMode.MOBILE : activeTab.getViewMode();
    }

    private void notifyTabsUpdated() {
        mTabFaviconStripController.update(
            mTabManager.getTabs(PROJECT_BROWSER_SESSION_HANDLE),
            getActiveTab());
    }

    private void handleMainFrameError() {
        mWebViewCover.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void show() {
        mVisible = true;
        mOverlayContainer.setVisibility(View.VISIBLE);
        WebView displayedWebView = mWebViewHost.getDisplayedWebView();
        if (displayedWebView != null) displayedWebView.requestFocus();
        updateOverviewActionsVisibility();
    }

    public void hide() {
        mVisible = false;
        mProgressBar.setVisibility(View.GONE);
        mWebViewCover.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
        mOverlayContainer.setVisibility(View.GONE);
        resetTabsToBlank();
        updateOverviewActionsVisibility();
    }

    private void resetTabsToBlank() {
        mCurrentUrl = null;
        mLoadedUrl = null;
        mHeaderUrlView.setText("");
        for (BrowserCoreWebChromeClient chromeClient : mWebChromeClientByTab.values()) {
            chromeClient.cancelPendingFileChooser();
        }
        mWebChromeClientByTab.clear();
        mTabManager.removeSession(PROJECT_BROWSER_SESSION_HANDLE);
        mWebViewHost.removeSession(PROJECT_BROWSER_SESSION_HANDLE);
        notifyTabsUpdated();
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
        WebView displayedWebView = mWebViewHost.getDisplayedWebView();
        if (displayedWebView != null && displayedWebView.canGoBack()) {
            WebBackForwardList backForwardList = displayedWebView.copyBackForwardList();
            int previousIndex = backForwardList.getCurrentIndex() - 1;
            if (previousIndex >= 0 && "about:blank".equals(backForwardList.getItemAtIndex(previousIndex).getUrl())) {
                hide();
            } else {
                displayedWebView.goBack();
            }
            return true;
        }
        hide();
        return true;
    }

    public void onActivityDestroy() {
        for (BrowserCoreWebChromeClient chromeClient : mWebChromeClientByTab.values()) {
            chromeClient.cancelPendingFileChooser();
        }
        mWebChromeClientByTab.clear();
        mDownloadController.unregisterDownloadCompleteReceiver();
        mWebViewHost.destroyAll();
    }
}
