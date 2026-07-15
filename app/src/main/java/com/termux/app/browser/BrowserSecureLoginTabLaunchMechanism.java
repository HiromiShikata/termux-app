package com.termux.app.browser;

public enum BrowserSecureLoginTabLaunchMechanism {

    AUTH_TAB,

    CUSTOM_TAB,

    EXTERNAL_CHROME;

    public static BrowserSecureLoginTabLaunchMechanism resolve(
            boolean authTabApiAvailable,
            boolean customTabsProviderAvailable) {
        if (authTabApiAvailable && customTabsProviderAvailable) {
            return AUTH_TAB;
        }
        if (customTabsProviderAvailable) {
            return CUSTOM_TAB;
        }
        return EXTERNAL_CHROME;
    }
}
