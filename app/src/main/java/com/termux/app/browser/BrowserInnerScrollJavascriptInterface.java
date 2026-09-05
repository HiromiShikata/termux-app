package com.termux.app.browser;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

public final class BrowserInnerScrollJavascriptInterface {

    public static final String INTERFACE_NAME = "TermuxScrollBridge";

    public static final String INJECTION_SCRIPT =
        "(function(){" +
        "if(window.__tmxScrollInstalled)return;" +
        "window.__tmxScrollInstalled=true;" +
        "var s=new Set();" +
        "document.addEventListener('scroll',function(e){" +
        "var t=e.target;" +
        "if(t&&t.scrollTop>0){s.add(t);}else if(t){s.delete(t);}" +
        "var b=window.TermuxScrollBridge;" +
        "if(b)b.onScrolled(s.size>0);" +
        "},true);" +
        "})();";

    private final Handler mMainHandler;
    private final WebView mWebView;
    private final BrowserWebViewScrollTracker mScrollTracker;

    public BrowserInnerScrollJavascriptInterface(
            @NonNull WebView webView,
            @NonNull BrowserWebViewScrollTracker scrollTracker) {
        mMainHandler = new Handler(Looper.getMainLooper());
        mWebView = webView;
        mScrollTracker = scrollTracker;
    }

    @JavascriptInterface
    public void onScrolled(boolean hasContentAbove) {
        mMainHandler.post(() -> mScrollTracker.recordInnerScrollHasContentAbove(mWebView, hasContentAbove));
    }
}
