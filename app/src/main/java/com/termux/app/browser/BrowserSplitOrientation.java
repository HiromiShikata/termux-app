package com.termux.app.browser;

public enum BrowserSplitOrientation {

    PORTRAIT,

    LANDSCAPE;

    public static BrowserSplitOrientation resolve(int configurationOrientation, int landscapeConfigurationOrientation) {
        return configurationOrientation == landscapeConfigurationOrientation ? LANDSCAPE : PORTRAIT;
    }

    public boolean isLandscape() {
        return this == LANDSCAPE;
    }

    public boolean ratioAppliesToWidth() {
        return isLandscape();
    }

    public boolean dividerTracksHorizontalAxis() {
        return isLandscape();
    }
}
