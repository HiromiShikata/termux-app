package com.termux.app.browser;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

public final class BrowserPdfDocumentPagesView {

    private BrowserPdfDocumentPagesView() {
    }

    @NonNull
    public static View create(@NonNull Context context, @NonNull BrowserPdfDocumentPages pages,
                              int widthPixels) {
        return new LinearLayout(context);
    }
}
