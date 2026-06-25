package com.termux.app.browser;

public enum BrowserViewMode {
    MOBILE,
    DESKTOP;

    public boolean isDesktop() {
        return this == DESKTOP;
    }

    public boolean isMobile() {
        return this == MOBILE;
    }
}
