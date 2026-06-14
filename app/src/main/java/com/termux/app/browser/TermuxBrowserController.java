package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TermuxBrowserController implements BrowserTabSelectionListener {

    private static final String LOG_TAG = "TermuxBrowserController";

    private static final float MIN_BROWSER_SPLIT_RATIO = 0.2f;

    private static final float MAX_BROWSER_SPLIT_RATIO = 0.85f;

    private final TermuxActivity mActivity;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final BrowserSessionVisibilityState mSessionVisibilityState = new BrowserSessionVisibilityState();

    private final WebView mWebView;

    private final View mBrowserContentContainer;

    private final View mBrowserTerminalDivider;

    private float mSplitDragStartRawY;

    private int mSplitDragStartBrowserHeight;

    private int mSplitDragTotalHeight;

    private final TextView mPageTitleUrlHeaderView;

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final ProgressBar mPageLoadProgressBar;

    private final View mWebViewCover;

    private final ListView mTabsListView;

    private CheckBox mDesktopModeToggle;

    private String mDefaultUserAgent;

    private BrowserTabsListViewController mTabsListViewController;

    private final BrowserProjectUrlButtonsViewController mProjectUrlButtonsViewController;

    private final View mProjectOverviewActionsView;

    private String mCurrentSessionHandle;

    private BrowserTab mDisplayedTab;

    private boolean mBrowserVisible;

    private String mLoadedUrl;

    private final Set<Long> mEnqueuedDownloadIds = new HashSet<>();

    private BroadcastReceiver mDownloadCompleteReceiver;

    private boolean mDownloadReceiverRegistered;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebView = activity.findViewById(R.id.browser_web_view);
        this.mBrowserContentContainer = activity.findViewById(R.id.browser_content_container);
        this.mBrowserTerminalDivider = activity.findViewById(R.id.browser_terminal_divider);
        this.mPageTitleUrlHeaderView = activity.findViewById(R.id.browser_page_title_url_header);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.browser_swipe_refresh);
        this.mPageLoadProgressBar = activity.findViewById(R.id.browser_page_load_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.browser_web_view_cover);
        this.mTabsListView = activity.findViewById(R.id.browser_tabs_list);
        this.mProjectUrlButtonsViewController = new BrowserProjectUrlButtonsViewController(activity, this);
        this.mProjectOverviewActionsView = activity.findViewById(R.id.browser_project_overview_actions);
        configureWebView();
        configureCookies();
        configureDrawerControls();
        configureProjectOverviewActions();
        configureBrowserSplitDivider();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureBrowserSplitDivider() {
        mBrowserTerminalDivider.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mSplitDragStartRawY = event.getRawY();
                    mSplitDragStartBrowserHeight = mBrowserContentContainer.getHeight();
                    mSplitDragTotalHeight = mSplitDragStartBrowserHeight + mActivity.getTerminalView().getHeight();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mSplitDragTotalHeight <= 0) return true;
                    float draggedBrowserHeight = mSplitDragStartBrowserHeight + (event.getRawY() - mSplitDragStartRawY);
                    applyBrowserSplitRatio(draggedBrowserHeight / mSplitDragTotalHeight);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    LinearLayout.LayoutParams browserParams =
                        (LinearLayout.LayoutParams) mBrowserContentContainer.getLayoutParams();
                    mActivity.getPreferences().setBrowserSplitRatio(clampBrowserSplitRatio(browserParams.weight));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void applyBrowserSplitRatio(float ratio) {
        float clampedRatio = clampBrowserSplitRatio(ratio);
        LinearLayout.LayoutParams browserParams =
            (LinearLayout.LayoutParams) mBrowserContentContainer.getLayoutParams();
        browserParams.height = 0;
        browserParams.weight = clampedRatio;
        mBrowserContentContainer.setLayoutParams(browserParams);
        View terminalView = mActivity.getTerminalView();
        LinearLayout.LayoutParams terminalParams =
            (LinearLayout.LayoutParams) terminalView.getLayoutParams();
        terminalParams.height = 0;
        terminalParams.weight = 1f - clampedRatio;
        terminalView.setLayoutParams(terminalParams);
    }

    private float clampBrowserSplitRatio(float ratio) {
        return Math.max(MIN_BROWSER_SPLIT_RATIO, Math.min(MAX_BROWSER_SPLIT_RATIO, ratio));
    }

    private void showBrowserSplitDivider() {
        applyBrowserSplitRatio(mActivity.getPreferences().getBrowserSplitRatio());
        mBrowserTerminalDivider.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = mWebView.getSettings();
        mDefaultUserAgent = settings.getUserAgentString();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        applyDarkModeRendering(settings);
        BrowserWebAuthentication.apply(settings);

        mSwipeRefreshLayout.setOnRefreshListener(mWebView::reload);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showPageLoadProgress(0);
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    notifyTabsUpdated();
                }
                updatePageHeader();
                applyDesktopViewport(view, loadingTab);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                mLoadedUrl = url;
                revealWebView();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mLoadedUrl = url;
                revealWebView();
                hidePageLoadProgress();
                mSwipeRefreshLayout.setRefreshing(false);
                CookieManager.getInstance().flush();
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    loadingTab.setTitle(view.getTitle());
                    notifyTabsUpdated();
                }
                updatePageHeader();
                applyDesktopViewport(view, loadingTab);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    notifyTabsUpdated();
                }
                updatePageHeader();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) return;
                onMainFrameError();
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                onMainFrameError();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    showPageLoadProgress(newProgress);
                } else {
                    hidePageLoadProgress();
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setTitle(title);
                    notifyTabsUpdated();
                }
                updatePageHeader();
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            enqueueDownload(url, userAgent, contentDisposition, mimetype));

        mWebView.setOnLongClickListener(view -> onWebViewLongPress());
    }

    private boolean onWebViewLongPress() {
        WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
        if (hitTestResult == null) return false;
        int hitTestType = hitTestResult.getType();
        if (!BrowserLinkLongPress.isLinkHit(hitTestType)) return false;
        if (BrowserLinkLongPress.requiresHrefLookup(hitTestType)) {
            requestLinkHrefThenShowMenu();
            return true;
        }
        String linkUrl = hitTestResult.getExtra();
        if (!BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) return false;
        showLinkContextMenu(linkUrl);
        return true;
    }

    private void requestLinkHrefThenShowMenu() {
        Handler hrefHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                String linkUrl = message.getData().getString("url");
                if (BrowserLinkLongPress.isOpenableLinkUrl(linkUrl)) {
                    showLinkContextMenu(linkUrl);
                }
            }
        };
        mWebView.requestFocusNodeHref(hrefHandler.obtainMessage());
    }

    private void showLinkContextMenu(@NonNull String linkUrl) {
        CharSequence[] actions = {
            mActivity.getString(R.string.action_browser_open_link_in_new_tab),
            mActivity.getString(R.string.action_browser_open_in_chrome)
        };
        new AlertDialog.Builder(mActivity)
            .setTitle(linkUrl)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    openUrlInNewTab(linkUrl);
                } else {
                    ShareUtils.openUrlInChrome(mActivity, linkUrl);
                }
            })
            .show();
    }

    private void applyDarkModeRendering(@NonNull WebSettings settings) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int forceDarkMode = ThemeUtils.isNightModeEnabled(mActivity)
                ? WebSettingsCompat.FORCE_DARK_ON
                : WebSettingsCompat.FORCE_DARK_OFF;
            WebSettingsCompat.setForceDark(settings, forceDarkMode);
        }
    }

    private void onMainFrameError() {
        revealWebView();
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void showWebViewCover() {
        mWebViewCover.setVisibility(View.VISIBLE);
    }

    private void revealWebView() {
        mWebViewCover.setVisibility(View.GONE);
    }

    private void showPageLoadProgress(int progress) {
        if (!mBrowserVisible) return;
        mPageLoadProgressBar.setProgress(progress);
        mPageLoadProgressBar.setVisibility(View.VISIBLE);
    }

    private void hidePageLoadProgress() {
        mPageLoadProgressBar.setVisibility(View.GONE);
    }

    private void updatePageHeader() {
        BrowserTab displayedTab = mDisplayedTab;
        String headerText = (displayedTab == null)
            ? ""
            : BrowserPageHeaderText.format(displayedTab.getTitle(), displayedTab.getUrl());
        mPageTitleUrlHeaderView.setText(headerText);
        boolean darkTheme =
            ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());
        mPageTitleUrlHeaderView.setTextColor(darkTheme ? Color.WHITE : Color.BLACK);
        updateProjectOverviewActionsVisibility();
    }

    private void enqueueDownload(@Nullable String url, @Nullable String userAgent,
                                 @Nullable String contentDisposition, @Nullable String mimetype) {
        DownloadManager downloadManager =
            (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null || url == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
            return;
        }
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (!TextUtils.isEmpty(mimetype)) {
                request.setMimeType(mimetype);
            }
            if (!TextUtils.isEmpty(userAgent)) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            String cookie = CookieManager.getInstance().getCookie(url);
            if (!TextUtils.isEmpty(cookie)) {
                request.addRequestHeader("Cookie", cookie);
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            boolean receiverReady = registerDownloadCompleteReceiver();
            long downloadId = downloadManager.enqueue(request);
            if (receiverReady) {
                mEnqueuedDownloadIds.add(downloadId);
            }
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_started, fileName), false);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to enqueue browser download", e);
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
        }
    }

    private boolean registerDownloadCompleteReceiver() {
        if (mDownloadReceiverRegistered) return true;
        mDownloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (!mEnqueuedDownloadIds.remove(completedId)) return;
                if (isDownloadSuccessful(completedId)) {
                    openDownloadsView();
                } else {
                    mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        try {
            ContextCompat.registerReceiver(mActivity, mDownloadCompleteReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
            mDownloadReceiverRegistered = true;
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to register download receiver", e);
            mDownloadCompleteReceiver = null;
            return false;
        }
    }

    private void unregisterDownloadCompleteReceiver() {
        if (!mDownloadReceiverRegistered) return;
        try {
            mActivity.unregisterReceiver(mDownloadCompleteReceiver);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to unregister download receiver", e);
        }
        mDownloadReceiverRegistered = false;
    }

    private boolean isDownloadSuccessful(long downloadId) {
        DownloadManager downloadManager =
            (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return false;
        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) return false;
            int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusColumn < 0) return false;
            return cursor.getInt(statusColumn) == DownloadManager.STATUS_SUCCESSFUL;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to query download status", e);
            return false;
        }
    }

    private void openDownloadsView() {
        if (!mActivity.isVisible()) return;
        try {
            mActivity.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_complete), false);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open downloads view", e);
        }
    }

    private void configureCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
    }

    private void configureDrawerControls() {
        mActivity.findViewById(R.id.browser_new_tab_button).setOnClickListener(v -> promptNewTab());
        mActivity.findViewById(R.id.browser_open_in_chrome_button).setOnClickListener(v -> openCurrentPageInChrome());
        mDesktopModeToggle = mActivity.findViewById(R.id.browser_desktop_mode_toggle);
        mDesktopModeToggle.setOnClickListener(v -> toggleActiveTabDesktopMode());
    }

    private void openCurrentPageInChrome() {
        String currentUrl = mWebView.getUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        ShareUtils.openUrlInChrome(mActivity, currentUrl);
    }

    private void configureProjectOverviewActions() {
        mActivity.findViewById(R.id.browser_open_all_tasks_button)
            .setOnClickListener(v -> openDisplayedTaskUrls(0));
        mActivity.findViewById(R.id.browser_open_first_ten_tasks_button)
            .setOnClickListener(v -> openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    private void openDisplayedTaskUrls(int limit) {
        mWebView.evaluateJavascript(BrowserGithubTaskUrls.COLLECT_SCRIPT, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String collectedUrlsJson) {
                List<String> displayedUrls;
                try {
                    displayedUrls = BrowserGithubTaskUrls.parseCollectedUrls(collectedUrlsJson);
                } catch (JSONException e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to parse collected task URLs", e);
                    return;
                }
                List<String> selectedUrls = BrowserGithubTaskUrls.selectForBulkOpen(displayedUrls, limit);
                openUrlsInExternalBrowser(selectedUrls);
            }
        });
    }

    private void openUrlsInExternalBrowser(@NonNull List<String> urls) {
        if (urls.isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_task_urls_found), false);
            return;
        }
        for (String url : urls) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                mActivity.startActivity(viewIntent);
            } catch (ActivityNotFoundException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "No external browser found to open " + url, e);
            }
        }
    }

    private void updateProjectOverviewActionsVisibility() {
        BrowserTab displayedTab = mDisplayedTab;
        boolean showActions = mBrowserVisible
            && displayedTab != null
            && BrowserProjectOverviewPage.isOverviewUrl(displayedTab.getUrl());
        mProjectOverviewActionsView.setVisibility(showActions ? View.VISIBLE : View.GONE);
    }

    private void toggleActiveTabDesktopMode() {
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            updateDesktopModeToggleState();
            return;
        }
        activeTab.setDesktopMode(!activeTab.isDesktopMode());
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        mDisplayedTab = activeTab;
        mWebView.reload();
    }

    private void applyUserAgent(@NonNull BrowserTab tab) {
        mWebView.getSettings().setUserAgentString(
            BrowserUserAgent.resolve(tab.isDesktopMode(), mDefaultUserAgent));
    }

    private void applyDesktopViewport(@NonNull WebView view, @Nullable BrowserTab tab) {
        if (!BrowserDesktopViewport.appliesTo(tab)) return;
        view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
    }

    private void updateDesktopModeToggleState() {
        if (mDesktopModeToggle == null) return;
        BrowserTab activeTab = getActiveTab();
        mDesktopModeToggle.setChecked(activeTab != null && activeTab.isDesktopMode());
    }

    public void onSessionChanged(@Nullable TerminalSession session) {
        String newSessionHandle = (session == null) ? null : session.mHandle;
        boolean switchingSession =
            BrowserSessionSwitch.requiresTerminalOnSessionChange(mCurrentSessionHandle, newSessionHandle);
        if (switchingSession) {
            mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mBrowserVisible);
        }
        mCurrentSessionHandle = newSessionHandle;
        rebindTabsList(session);
        updateDesktopModeToggleState();
        if (switchingSession) {
            restoreSessionVisibility();
            return;
        }
        if (mBrowserVisible) {
            if (getActiveTab() != null) {
                loadActiveTab();
            } else {
                showTerminal();
            }
        }
    }

    private void restoreSessionVisibility() {
        boolean restoreBrowser = mSessionVisibilityState.shouldRestoreBrowserOnSessionChange(
            mCurrentSessionHandle, getActiveTab() != null);
        if (restoreBrowser) {
            showBrowser();
        } else {
            showTerminal();
        }
    }

    private void rebindTabsList(@Nullable TerminalSession session) {
        if (mCurrentSessionHandle == null) {
            mTabsListView.setAdapter(null);
            mProjectUrlButtonsViewController.showProjectUrlsForSession(null);
            return;
        }

        com.google.android.material.textview.MaterialTextView headerView =
            mActivity.findViewById(R.id.browser_drawer_session_name);
        headerView.setText(sessionDisplayName(session));

        List<BrowserTab> tabs = mTabManager.getTabs(mCurrentSessionHandle);
        mTabsListViewController = new BrowserTabsListViewController(mActivity, this, tabs);
        mTabsListView.setAdapter(mTabsListViewController);
        mTabsListView.setOnItemClickListener(mTabsListViewController);

        String sessionName = (session == null) ? null : session.mSessionName;
        mProjectUrlButtonsViewController.showProjectUrlsForSession(sessionName);
    }

    private String sessionDisplayName(@Nullable TerminalSession session) {
        if (session == null) return mActivity.getString(R.string.title_browser_tabs);
        if (session.mSessionName != null && !session.mSessionName.isEmpty()) return session.mSessionName;
        String title = session.getTitle();
        return (title == null || title.isEmpty()) ? mActivity.getString(R.string.title_browser_tabs) : title;
    }

    public void onSessionRemoved(@NonNull TerminalSession session) {
        if (mDisplayedTab != null && session.mHandle.equals(mDisplayedTab.getSessionHandle()))
            mDisplayedTab = null;
        mTabManager.removeSession(session.mHandle);
        mSessionVisibilityState.clearSession(session.mHandle);
    }

    public void toggleBrowser() {
        if (mBrowserVisible) {
            showTerminal();
        } else {
            showBrowser();
        }
    }

    public void showBrowser() {
        if (mCurrentSessionHandle == null) return;
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            promptNewTab();
            return;
        }
        mBrowserVisible = true;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, true);
        loadActiveTab();
        updatePageHeader();
        mBrowserContentContainer.setVisibility(View.VISIBLE);
        showBrowserSplitDivider();
        dismissSoftKeyboardForBrowser();
    }

    private void dismissSoftKeyboardForBrowser() {
        EditText toolbarTextInput = mActivity.getTerminalToolbarTextInput();
        if (toolbarTextInput != null && toolbarTextInput.hasFocus())
            toolbarTextInput.clearFocus();
        View focusedView = mActivity.getCurrentFocus();
        View keyboardTargetView = focusedView != null ? focusedView : mActivity.getTerminalView();
        KeyboardUtils.hideSoftKeyboard(mActivity, keyboardTargetView);
    }

    public void showTerminal() {
        mBrowserVisible = false;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, false);
        revealWebView();
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
        mBrowserContentContainer.setVisibility(View.GONE);
        mBrowserTerminalDivider.setVisibility(View.GONE);
        updateProjectOverviewActionsVisibility();
    }

    public boolean isBrowserVisible() {
        return mBrowserVisible;
    }

    public void openTab(@NonNull BrowserTab tab) {
        boolean browserWasHidden = !mBrowserVisible;
        mTabManager.setActiveTab(tab);
        mBrowserVisible = true;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, true);
        loadActiveTab(browserWasHidden);
        updatePageHeader();
        mBrowserContentContainer.setVisibility(View.VISIBLE);
        showBrowserSplitDivider();
        dismissSoftKeyboardForBrowser();
        notifyTabsUpdated();
        mActivity.getDrawer().closeDrawers();
    }

    public void closeTab(@NonNull BrowserTab tab) {
        mTabManager.removeTab(tab);
        if (tab == mDisplayedTab) mDisplayedTab = null;
        notifyTabsUpdated();
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            updateDesktopModeToggleState();
            showTerminal();
        } else if (mBrowserVisible) {
            loadActiveTab();
        } else {
            updateDesktopModeToggleState();
        }
    }

    public void openUrlInNewTab(@NonNull String url) {
        if (mCurrentSessionHandle == null) return;
        BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, normalizeUrl(url));
        openTab(tab);
    }

    public void attachBackgroundTab(@NonNull String sessionHandle, @NonNull String url) {
        String normalizedUrl = normalizeUrl(url);
        if (mTabManager.findTabByUrl(sessionHandle, normalizedUrl) != null) return;
        mTabManager.addTab(sessionHandle, normalizedUrl);
        if (sessionHandle.equals(mCurrentSessionHandle)) notifyTabsUpdated();
    }

    private void promptNewTab() {
        if (mCurrentSessionHandle == null) return;
        TextInputDialogUtils.textInput(mActivity, R.string.title_browser_open_url, null,
            R.string.action_browser_open_url_confirm, text -> {
                if (mCurrentSessionHandle == null) return;
                String url = normalizeUrl(text);
                BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, url);
                openTab(tab);
            },
            -1, null, android.R.string.cancel, null, null);
    }

    private static String normalizeUrl(@Nullable String input) {
        if (input == null) return BrowserTab.DEFAULT_URL;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return BrowserTab.DEFAULT_URL;
        if (trimmed.contains("://")) return trimmed;
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            return "https://duckduckgo.com/?q=" + android.net.Uri.encode(trimmed);
        }
        return "https://" + trimmed;
    }

    private void loadActiveTab() {
        loadActiveTab(false);
    }

    private void loadActiveTab(boolean forceReload) {
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) return;
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        if (!forceReload && activeTab == mDisplayedTab) return;
        String targetUrl = activeTab.getUrl();
        if (BrowserPageTransition.requiresCoverWhileLoading(mLoadedUrl, targetUrl, mBrowserVisible)) {
            showWebViewCover();
        }
        mDisplayedTab = activeTab;
        updatePageHeader();
        mWebView.loadUrl(targetUrl);
    }

    @Nullable
    public BrowserTab getActiveTab() {
        if (mCurrentSessionHandle == null) return null;
        return mTabManager.getActiveTab(mCurrentSessionHandle);
    }

    private void notifyTabsUpdated() {
        if (mTabsListViewController != null) mTabsListViewController.notifyDataSetChanged();
    }

    public boolean onBackPressed() {
        if (mBrowserVisible && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        if (mBrowserVisible) {
            showTerminal();
            return true;
        }
        return false;
    }

    public void toggleTabsDrawer() {
        DrawerLayout drawer = mActivity.getDrawer();
        if (drawer.isDrawerOpen(Gravity.END))
            drawer.closeDrawer(Gravity.END);
        else
            drawer.openDrawer(Gravity.END);
    }

    public void onActivityStop() {
        try {
            CookieManager.getInstance().flush();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to flush cookies", e);
        }
    }

    public void onActivityDestroy() {
        unregisterDownloadCompleteReceiver();
        mWebView.stopLoading();
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("about:blank");
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}
