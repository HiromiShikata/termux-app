package com.termux.app.browser;

import android.content.Context;
import android.view.ViewStructure;
import android.webkit.WebView;

import androidx.annotation.NonNull;

public final class BrowserAssistStructureFreeWebView extends WebView {

    public BrowserAssistStructureFreeWebView(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onProvideVirtualStructure(ViewStructure structure) {
        structure.setChildCount(0);
    }
}
