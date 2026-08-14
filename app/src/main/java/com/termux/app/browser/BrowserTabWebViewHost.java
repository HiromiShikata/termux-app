package com.termux.app.browser;

import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class BrowserTabWebViewHost {

    public interface WebViewFactory {
        @NonNull
        WebView createWebViewForTab(@NonNull BrowserTab tab);
    }

    public interface WebViewDestroyListener {
        void onWebViewDestroyed(@NonNull WebView webView);
    }

    private final FrameLayout mContainer;

    private final WebViewFactory mWebViewFactory;

    @Nullable
    private WebViewDestroyListener mWebViewDestroyListener;

    private final int mLiveWebViewWindowSize;

    private final Map<BrowserTab, WebView> mWebViewByTab = new LinkedHashMap<>();

    private final Set<WebView> mDestroyedWebViews = Collections.newSetFromMap(new WeakHashMap<>());

    private BrowserTab mDisplayedTab;

    public BrowserTabWebViewHost(@NonNull FrameLayout container, @NonNull WebViewFactory webViewFactory) {
        this(container, webViewFactory, BrowserLiveWebViewWindowPlanner.DEFAULT_WINDOW_SIZE);
    }

    public BrowserTabWebViewHost(@NonNull FrameLayout container, @NonNull WebViewFactory webViewFactory,
                                 int liveWebViewWindowSize) {
        mContainer = container;
        mWebViewFactory = webViewFactory;
        mLiveWebViewWindowSize = Math.max(1, liveWebViewWindowSize);
    }

    public boolean hasWebViewForTab(@NonNull BrowserTab tab) {
        return mWebViewByTab.containsKey(tab);
    }

    @NonNull
    public WebView showTab(@NonNull BrowserTab tab) {
        boolean firstDisplay = !mWebViewByTab.containsKey(tab);
        WebView webView = webViewForTab(tab);
        touchMostRecentlyUsed(tab, webView);
        for (Map.Entry<BrowserTab, WebView> entry : mWebViewByTab.entrySet()) {
            entry.getValue().setVisibility(entry.getKey() == tab ? View.VISIBLE : View.GONE);
        }
        mDisplayedTab = tab;
        if (firstDisplay) {
            webView.loadUrl(tab.getUrl());
        }
        enforceLiveWebViewWindow();
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

    private void touchMostRecentlyUsed(@NonNull BrowserTab tab, @NonNull WebView webView) {
        mWebViewByTab.remove(tab);
        mWebViewByTab.put(tab, webView);
    }

    private void enforceLiveWebViewWindow() {
        List<BrowserTab> liveTabsMostRecentFirst = new ArrayList<>(mWebViewByTab.keySet());
        Collections.reverse(liveTabsMostRecentFirst);
        BrowserLiveWebViewWindowPlanner<BrowserTab> plan = BrowserLiveWebViewWindowPlanner.resolve(
            mDisplayedTab, liveTabsMostRecentFirst, mLiveWebViewWindowSize);
        for (BrowserTab tab : plan.getOwnersToRelease()) {
            releaseWebViewForTab(tab);
        }
    }

    private void releaseWebViewForTab(@NonNull BrowserTab tab) {
        if (tab == mDisplayedTab) return;
        WebView webView = mWebViewByTab.remove(tab);
        if (webView != null) destroyWebView(webView);
    }

    @Nullable
    public WebView getDisplayedWebView() {
        if (mDisplayedTab == null) return null;
        return mWebViewByTab.get(mDisplayedTab);
    }

    public int getLiveWebViewCount() {
        return mWebViewByTab.size();
    }

    @NonNull
    public List<WebView> getAllWebViews() {
        return new ArrayList<>(mWebViewByTab.values());
    }

    @Nullable
    public BrowserTab getDisplayedTab() {
        return mDisplayedTab;
    }

    @Nullable
    public BrowserTab findTabForWebView(@NonNull WebView webView) {
        for (Map.Entry<BrowserTab, WebView> entry : mWebViewByTab.entrySet()) {
            if (entry.getValue() == webView) return entry.getKey();
        }
        return null;
    }

    @NonNull
    public WebView recreateWebViewForTab(@NonNull BrowserTab tab) {
        return recreateWebViewForTabLoadingUrl(tab, tab.getUrl());
    }

    @NonNull
    public WebView recreateWebViewForTabWithBlankPage(@NonNull BrowserTab tab) {
        return recreateWebViewForTabLoadingUrl(tab, "about:blank");
    }

    @NonNull
    private WebView recreateWebViewForTabLoadingUrl(@NonNull BrowserTab tab, @NonNull String urlToLoad) {
        WebView deadWebView = mWebViewByTab.remove(tab);
        if (deadWebView != null) destroyDeadWebView(deadWebView);
        boolean displayed = tab == mDisplayedTab;
        WebView webView = mWebViewFactory.createWebViewForTab(tab);
        webView.setVisibility(displayed ? View.VISIBLE : View.GONE);
        mContainer.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        mWebViewByTab.put(tab, webView);
        webView.loadUrl(urlToLoad);
        return webView;
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
        if (!markDestroyedIfLive(webView)) return;
        webView.stopLoading();
        webView.loadUrl("about:blank");
        mContainer.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
    }

    private void destroyDeadWebView(@NonNull WebView webView) {
        if (!markDestroyedIfLive(webView)) return;
        mContainer.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
    }

    public void setWebViewDestroyListener(@Nullable WebViewDestroyListener listener) {
        mWebViewDestroyListener = listener;
    }

    private boolean markDestroyedIfLive(@NonNull WebView webView) {
        boolean newlyDestroyed = mDestroyedWebViews.add(webView);
        if (newlyDestroyed && mWebViewDestroyListener != null) {
            mWebViewDestroyListener.onWebViewDestroyed(webView);
        }
        return newlyDestroyed;
    }

    public boolean isWebViewDestroyed(@NonNull WebView webView) {
        return mDestroyedWebViews.contains(webView);
    }

    public boolean canRunLifecycleCallOn(@Nullable WebView webView) {
        return webView != null
            && Looper.myLooper() == Looper.getMainLooper()
            && !mDestroyedWebViews.contains(webView)
            && webView.getParent() != null;
    }
}
