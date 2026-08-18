package com.termux.app.browser;

import android.content.Context;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.webkit.WebView;

import androidx.annotation.NonNull;

final class BrowserClipboardImagePasteInputConnection extends InputConnectionWrapper {

    private final Context mContext;
    private final WebView mWebView;

    BrowserClipboardImagePasteInputConnection(@NonNull InputConnection target,
                                              @NonNull Context context,
                                              @NonNull WebView webView) {
        super(target, false);
        this.mContext = context;
        this.mWebView = webView;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        if (id == android.R.id.paste
                && BrowserClipboardImagePaste.pasteIfImageInClipboard(mContext, mWebView)) {
            return true;
        }
        return super.performContextMenuAction(id);
    }
}
