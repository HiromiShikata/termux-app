package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import java.util.List;

public final class TermuxBrowserController {

    private static final String LOG_TAG = "TermuxBrowserController";

    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";

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
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                hidePageLoadProgress();
                mSwipeRefreshLayout.setRefreshing(false);
                CookieManager.getInstance().flush();
                BrowserTab loadingTab = mDisplayedTab;
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    loadingTab.setTitle(view.getTitle());
                    notifyTabsUpdated();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                hidePageLoadProgress();
                mSwipeRefreshLayout.setRefreshing(false);
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
        });
    }

    private void showPageLoadProgress(int progress) {
        mPageLoadProgressBar.setProgress(progress);
        mPageLoadProgressBar.setVisibility(View.VISIBLE);
    }

    private void hidePageLoadProgress() {
        mPageLoadProgressBar.setVisibility(View.GONE);
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
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
        loadActiveTab();
    }

    public void showTerminal() {
        mBrowserVisible = false;
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
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
        loadActiveTab(browserWasHidden);
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
        if (!mTabManager.canAddTab(mCurrentSessionHandle)) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_max_tabs_reached), true);
            return;
        }
        BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, normalizeUrl(url));
        if (tab == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_max_tabs_reached), true);
            return;
        }
        openTab(tab);
    }

    public void attachBackgroundTab(@NonNull String sessionHandle, @NonNull String url) {
        String normalizedUrl = normalizeUrl(url);
        if (mTabManager.findTabByUrl(sessionHandle, normalizedUrl) != null) return;
        if (!mTabManager.canAddTab(sessionHandle)) return;
        mTabManager.addTab(sessionHandle, normalizedUrl);
        if (sessionHandle.equals(mCurrentSessionHandle)) notifyTabsUpdated();
    }

    private void promptNewTab() {
        if (mCurrentSessionHandle == null) return;
        if (!mTabManager.canAddTab(mCurrentSessionHandle)) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_max_tabs_reached), true);
            return;
        }
        TextInputDialogUtils.textInput(mActivity, R.string.title_browser_open_url, null,
            R.string.action_browser_open_url_confirm, text -> {
                String url = normalizeUrl(text);
                BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, url);
                if (tab == null) {
                    mActivity.showToast(mActivity.getString(R.string.msg_browser_max_tabs_reached), true);
                    return;
                }
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
        mDisplayedTab = activeTab;
        mWebView.loadUrl(activeTab.getUrl());
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
        mWebView.stopLoading();
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("about:blank");
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}
