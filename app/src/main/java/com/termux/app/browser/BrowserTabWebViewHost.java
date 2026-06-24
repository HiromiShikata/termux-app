package com.termux.app.browser;

import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BrowserTabWebViewHost {

    public interface WebViewFactory {
        @NonNull
        WebView createWebViewForTab(@NonNull BrowserTab tab);
    }

    private final FrameLayout mContainer;

    private final WebViewFactory mWebViewFactory;

    private final Map<BrowserTab, WebView> mWebViewByTab = new IdentityHashMap<>();

    private BrowserTab mDisplayedTab;

    public BrowserTabWebViewHost(@NonNull FrameLayout container, @NonNull WebViewFactory webViewFactory) {
        mContainer = container;
        mWebViewFactory = webViewFactory;
    }

    public boolean hasWebViewForTab(@NonNull BrowserTab tab) {
        return mWebViewByTab.containsKey(tab);
    }

    @NonNull
    public WebView showTab(@NonNull BrowserTab tab) {
        boolean firstDisplay = !mWebViewByTab.containsKey(tab);
        WebView webView = webViewForTab(tab);
        for (Map.Entry<BrowserTab, WebView> entry : mWebViewByTab.entrySet()) {
            entry.getValue().setVisibility(entry.getKey() == tab ? View.VISIBLE : View.GONE);
        }
        mDisplayedTab = tab;
        if (firstDisplay) {
            webView.loadUrl(tab.getUrl());
        }
        return webView;
    }

    @NonNull
    private WebView webViewForTab(@NonNull BrowserTab tab) {
        WebView existing = mWebViewByTab.get(tab);
        if (existing != null) return existing;
        WebView webView = mWebViewFactory.createWebViewForTab(tab);
        webView.setVisibility(View.GONE);
        mContainer.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        mWebViewByTab.put(tab, webView);
        return webView;
    }

    @Nullable
    public WebView getDisplayedWebView() {
        if (mDisplayedTab == null) return null;
        return mWebViewByTab.get(mDisplayedTab);
    }

    @Nullable
    public BrowserTab getDisplayedTab() {
        return mDisplayedTab;
    }

    public void removeTab(@NonNull BrowserTab tab) {
        WebView webView = mWebViewByTab.remove(tab);
        if (tab == mDisplayedTab) mDisplayedTab = null;
        if (webView != null) destroyWebView(webView);
    }

    public void removeSession(@NonNull String sessionHandle) {
        List<BrowserTab> tabsToRemove = new ArrayList<>();
        for (BrowserTab tab : mWebViewByTab.keySet()) {
            if (sessionHandle.equals(tab.getSessionHandle())) tabsToRemove.add(tab);
        }
        for (BrowserTab tab : tabsToRemove) removeTab(tab);
    }

    public void destroyAll() {
        List<WebView> webViews = new ArrayList<>(mWebViewByTab.values());
        mWebViewByTab.clear();
        mDisplayedTab = null;
        for (WebView webView : webViews) destroyWebView(webView);
    }

    private void destroyWebView(@NonNull WebView webView) {
        webView.stopLoading();
        webView.loadUrl("about:blank");
        mContainer.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
    }
}
