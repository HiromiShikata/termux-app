package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BrowserTabSelectionListener {

    void openTab(@NonNull BrowserTab tab);

    void closeTab(@NonNull BrowserTab tab);

    @Nullable
    BrowserTab getActiveTab();
}
