package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import java.util.List;

public final class TermuxBrowserController {

    private static final String LOG_TAG = "TermuxBrowserController";

    private final TermuxActivity mActivity;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final WebView mWebView;

    private final ListView mTabsListView;

    private CheckBox mDesktopModeToggle;

    private String mDefaultUserAgent;

    private BrowserTabsListViewController mTabsListViewController;

    private final BrowserProjectUrlButtonsViewController mProjectUrlButtonsViewController;

    private String mCurrentSessionHandle;

    private boolean mBrowserVisible;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebView = activity.findViewById(R.id.browser_web_view);
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

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                BrowserTab activeTab = getActiveTab();
                if (activeTab != null) {
                    activeTab.setUrl(url);
                    notifyTabsUpdated();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                BrowserTab activeTab = getActiveTab();
                if (activeTab != null) {
                    activeTab.setUrl(url);
                    activeTab.setTitle(view.getTitle());
                    notifyTabsUpdated();
                }
            }
        });
    }

    private void configureCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
    }

    private void configureDrawerControls() {
        mActivity.findViewById(R.id.browser_new_tab_button).setOnClickListener(v -> promptNewTab());
        mDesktopModeToggle = mActivity.findViewById(R.id.browser_desktop_mode_toggle);
        mDesktopModeToggle.setOnClickListener(v -> toggleActiveTabDesktopMode());
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
        mWebView.loadUrl(activeTab.getUrl());
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
        mWebView.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
        loadActiveTab();
    }

    public void showTerminal() {
        mBrowserVisible = false;
        mWebView.setVisibility(View.GONE);
        mActivity.getTerminalView().setVisibility(View.VISIBLE);
    }

    public boolean isBrowserVisible() {
        return mBrowserVisible;
    }

    public void openTab(@NonNull BrowserTab tab) {
        mTabManager.setActiveTab(tab);
        mBrowserVisible = true;
        mWebView.setVisibility(View.VISIBLE);
        mActivity.getTerminalView().setVisibility(View.GONE);
        loadActiveTab();
        notifyTabsUpdated();
        mActivity.getDrawer().closeDrawers();
    }

    public void closeTab(@NonNull BrowserTab tab) {
        mTabManager.removeTab(tab);
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
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) return;
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        String currentUrl = mWebView.getUrl();
        if (currentUrl == null || !currentUrl.equals(activeTab.getUrl())) {
            mWebView.loadUrl(activeTab.getUrl());
        }
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
