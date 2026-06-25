package com.termux.app.browser;

import androidx.annotation.Nullable;

public final class BrowserHeaderUrlMenuEligibility {

    public static boolean canShowMenuFor(@Nullable String url) {
        if (url == null || url.isEmpty()) return false;
        return !"about:blank".equals(url);
    }

    private BrowserHeaderUrlMenuEligibility() {
    }
}
