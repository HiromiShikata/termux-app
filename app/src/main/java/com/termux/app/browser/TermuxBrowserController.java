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
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.theme.ThemeUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TermuxBrowserController implements BrowserTabSelectionListener {

    private static final String LOG_TAG = "TermuxBrowserController";

    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";

    private static final String BLANK_URL = "about:blank";

    private final TermuxActivity mActivity;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final WebView mWebView;

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final ProgressBar mPageLoadProgressBar;

    private final ListView mTabsListView;

    private CheckBox mDesktopModeToggle;

    private String mDefaultUserAgent;

    private BrowserTabsListViewController mTabsListViewController;

    private final BrowserProjectUrlButtonsViewController mProjectUrlButtonsViewController;

    private String mCurrentSessionHandle;

    private BrowserTab mDisplayedTab;

    private boolean mBrowserVisible;

    private boolean mClearingDisplayedPage;

    private final Set<Long> mEnqueuedDownloadIds = new HashSet<>();

    private BroadcastReceiver mDownloadCompleteReceiver;

    private boolean mDownloadReceiverRegistered;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebView = activity.findViewById(R.id.browser_web_view);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.browser_swipe_refresh);
        this.mPageLoadProgressBar = activity.findViewById(R.id.browser_page_load_progress_bar);
        this.mTabsListView = activity.findViewById(R.id.browser_tabs_list);
        this.mProjectUrlButtonsViewController = new BrowserProjectUrlButtonsViewController(activity, this);
        configureWebView();
        configureCookies();
        configureDrawerControls();
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

        mSwipeRefreshLayout.setOnRefreshListener(mWebView::reload);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (isClearingBlankLoad(url)) return;
                mClearingDisplayedPage = false;
                showPageLoadProgress(0);
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    notifyTabsUpdated();
                }
                applyDesktopViewport(view, loadingTab);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (isClearingBlankLoad(url)) {
                    mClearingDisplayedPage = false;
                    return;
                }
                hidePageLoadProgress();
                mSwipeRefreshLayout.setRefreshing(false);
                CookieManager.getInstance().flush();
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    loadingTab.setTitle(view.getTitle());
                    notifyTabsUpdated();
                }
                applyDesktopViewport(view, loadingTab);
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
                if (mClearingDisplayedPage) return;
                if (newProgress < 100) {
                    showPageLoadProgress(newProgress);
                } else {
                    hidePageLoadProgress();
                }
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
            mActivity.getString(R.string.action_browser_open_link_in_new_tab)
        };
        new AlertDialog.Builder(mActivity)
            .setTitle(linkUrl)
            .setItems(actions, (dialog, which) -> openUrlInNewTab(linkUrl))
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
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private boolean isClearingBlankLoad(@Nullable String url) {
        return mClearingDisplayedPage && BLANK_URL.equals(url);
    }

    private void showPageLoadProgress(int progress) {
        if (!mBrowserVisible) return;
        mPageLoadProgressBar.setProgress(progress);
        mPageLoadProgressBar.setVisibility(View.VISIBLE);
    }

    private void hidePageLoadProgress() {
        mPageLoadProgressBar.setVisibility(View.GONE);
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
        Intent chromeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl));
        chromeIntent.setPackage(CHROME_PACKAGE_NAME);
        try {
            mActivity.startActivity(chromeIntent);
        } catch (ActivityNotFoundException e) {
            ShareUtils.openUrl(mActivity, currentUrl);
        }
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
        mCurrentSessionHandle = (session == null) ? null : session.mHandle;
        rebindTabsList(session);
        updateDesktopModeToggleState();
        BrowserTab activeTab = getActiveTab();
        if (mBrowserVisible) {
            if (activeTab != null) {
                loadActiveTab();
            } else {
                showTerminal();
            }
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
        loadActiveTab();
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
    }

    public void showTerminal() {
        mBrowserVisible = false;
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setVisibility(View.GONE);
        mActivity.getTerminalView().setVisibility(View.VISIBLE);
    }

    public boolean isBrowserVisible() {
        return mBrowserVisible;
    }

    public void openTab(@NonNull BrowserTab tab) {
        boolean browserWasHidden = !mBrowserVisible;
        mTabManager.setActiveTab(tab);
        mBrowserVisible = true;
        loadActiveTab(browserWasHidden);
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
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
        boolean switchingDisplayedPage =
            BrowserPageTransition.requiresBlankBeforeLoad(mDisplayedTab, activeTab);
        mDisplayedTab = activeTab;
        if (switchingDisplayedPage) clearDisplayedPage();
        mWebView.loadUrl(activeTab.getUrl());
    }

    private void clearDisplayedPage() {
        mClearingDisplayedPage = true;
        mWebView.loadUrl(BLANK_URL);
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

    public void openTabsDrawer() {
        mActivity.getDrawer().openDrawer(Gravity.END);
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
