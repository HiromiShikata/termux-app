package com.termux.app.browser;

import android.content.Context;
import android.view.ViewStructure;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserAssistStructureFreeWebView extends WebView {

    public BrowserAssistStructureFreeWebView(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onProvideVirtualStructure(ViewStructure structure) {
        structure.setChildCount(0);
    }

    @Nullable
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection base = super.onCreateInputConnection(outAttrs);
        if (base == null) return null;
        return new BrowserClipboardImagePasteInputConnection(base, getContext(), this);
    }
}
