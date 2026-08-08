package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserPendingDocumentDisplay {

    public interface Display {
        void display(long downloadId);
    }

    public void rememberUntilTheActivityReturns(long downloadId) {
    }

    public void displayRememberedDocument(@NonNull Display display) {
    }
}
