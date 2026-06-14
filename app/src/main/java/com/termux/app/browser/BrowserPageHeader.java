package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserPageHeader {

    private final String contextLine;

    private final String titleLine;

    private final String compactUrlLine;

    public BrowserPageHeader(@NonNull String contextLine, @NonNull String titleLine,
                             @NonNull String compactUrlLine) {
        this.contextLine = contextLine;
        this.titleLine = titleLine;
        this.compactUrlLine = compactUrlLine;
    }

    @NonNull
    public String getContextLine() {
        return contextLine;
    }

    @NonNull
    public String getTitleLine() {
        return titleLine;
    }

    @NonNull
    public String getCompactUrlLine() {
        return compactUrlLine;
    }

    public boolean hasContextLine() {
        return !contextLine.isEmpty();
    }

    public boolean hasTitleLine() {
        return !titleLine.isEmpty();
    }

    public boolean hasCompactUrlLine() {
        return !compactUrlLine.isEmpty();
    }
}
