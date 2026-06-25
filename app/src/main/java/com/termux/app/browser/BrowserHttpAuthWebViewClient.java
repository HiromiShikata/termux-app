package com.termux.app.browser;

import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserHttpAuthWebViewClient extends WebViewClient {

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        BrowserHttpAuthDialog.show(view.getContext(), handler, host, realm);
    }
}
