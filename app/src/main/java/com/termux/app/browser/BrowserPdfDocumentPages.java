package com.termux.app.browser;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public interface BrowserPdfDocumentPages {

    int count();

    @NonNull
    Bitmap render(int pageIndex, int widthPixels);

    void close();
}
