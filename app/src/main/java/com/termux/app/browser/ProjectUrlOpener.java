package com.termux.app.browser;

import androidx.annotation.NonNull;

public interface ProjectUrlOpener {

    void openProjectUrl(@NonNull String url, @NonNull BrowserViewMode viewMode);
}
