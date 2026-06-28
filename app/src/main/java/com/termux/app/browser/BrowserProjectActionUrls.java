package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserProjectActionUrls {

    public static final BrowserProjectActionUrls EMPTY = new BrowserProjectActionUrls(null, null, null);

    @Nullable
    private final String overviewUrl;

    @Nullable
    private final String tdpmConsoleUrl;

    @Nullable
    private final String newIssueUrl;

    public BrowserProjectActionUrls(@Nullable String overviewUrl, @Nullable String tdpmConsoleUrl,
                                    @Nullable String newIssueUrl) {
        this.overviewUrl = emptyToNull(overviewUrl);
        this.tdpmConsoleUrl = emptyToNull(tdpmConsoleUrl);
        this.newIssueUrl = emptyToNull(newIssueUrl);
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    @Nullable
    public String getOverviewUrl() {
        return overviewUrl;
    }

    @Nullable
    public String getTdpmConsoleUrl() {
        return tdpmConsoleUrl;
    }

    @Nullable
    public String getNewIssueUrl() {
        return newIssueUrl;
    }
}
