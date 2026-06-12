package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    private BrowserTabsListViewController mTabsListViewController;

    private String mCurrentSessionHandle;

    private boolean mBrowserVisible;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebView = activity.findViewById(R.id.browser_web_view);
        this.mTabsListView = activity.findViewById(R.id.browser_tabs_list);
        configureWebView();
        configureCookies();
        configureDrawerControls();
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
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);
    }

    private void configureDrawerControls() {
        mActivity.findViewById(R.id.browser_new_tab_button).setOnClickListener(v -> promptNewTab());
    }

    public void onSessionChanged(@Nullable TerminalSession session) {
        mCurrentSessionHandle = (session == null) ? null : session.mHandle;
        rebindTabsList(session);
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
            return;
        }

        com.google.android.material.textview.MaterialTextView headerView =
            mActivity.findViewById(R.id.browser_drawer_session_name);
        headerView.setText(sessionDisplayName(session));

        List<BrowserTab> tabs = mTabManager.getTabs(mCurrentSessionHandle);
        mTabsListViewController = new BrowserTabsListViewController(mActivity, this, tabs);
        mTabsListView.setAdapter(mTabsListViewController);
        mTabsListView.setOnItemClickListener(mTabsListViewController);
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
            showTerminal();
        } else if (mBrowserVisible) {
            loadActiveTab();
        }
    }

    private void promptNewTab() {
        if (mCurrentSessionHandle == null) return;
        if (!mTabManager.canAddTab(mCurrentSessionHandle)) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_max_tabs_reached), true);
            return;
        }
        TextInputDialogUtils.textInput(mActivity, R.string.title_browser_open_url, BrowserTab.DEFAULT_URL,
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
}
