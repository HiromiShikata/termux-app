package com.termux.app.browser;

public enum BrowserViewMode {
    MOBILE,
    DESKTOP;

    public static BrowserViewMode forDesktopFlag(boolean desktopMode) {
        return desktopMode ? DESKTOP : MOBILE;
    }

    public boolean isDesktop() {
        return this == DESKTOP;
    }

    public boolean isMobile() {
        return this == MOBILE;
    }
}
