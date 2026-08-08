package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserPendingDocumentDisplay {

    public interface Display {
        void display(long downloadId);
    }

    private static final long NO_DOCUMENT = -1L;

    private long mDownloadId = NO_DOCUMENT;

    public void rememberUntilTheActivityReturns(long downloadId) {
        mDownloadId = downloadId;
    }

    public void displayRememberedDocument(@NonNull Display display) {
        if (mDownloadId == NO_DOCUMENT) return;
        long rememberedDownloadId = mDownloadId;
        mDownloadId = NO_DOCUMENT;
        display.display(rememberedDownloadId);
    }
}
