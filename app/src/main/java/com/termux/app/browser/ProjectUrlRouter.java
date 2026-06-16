package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ProjectUrlRouter {

    private final ProjectUrlOpener mProjectBrowserOpener;

    public ProjectUrlRouter(@NonNull ProjectUrlOpener projectBrowserOpener) {
        this.mProjectBrowserOpener = projectBrowserOpener;
    }

    public void route(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) return;
        mProjectBrowserOpener.openProjectUrl(BrowserUrlInput.normalize(url));
    }
}
