package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserRenderProcessGoneDecision {

    private final boolean mTabKnown;

    private final boolean mTabDisplayed;

    private final boolean mDidCrash;

    private BrowserRenderProcessGoneDecision(boolean tabKnown, boolean tabDisplayed, boolean didCrash) {
        this.mTabKnown = tabKnown;
        this.mTabDisplayed = tabDisplayed;
        this.mDidCrash = didCrash;
    }

    @NonNull
    public static BrowserRenderProcessGoneDecision forDiedWebView(boolean tabKnown, boolean tabDisplayed, boolean didCrash) {
        return new BrowserRenderProcessGoneDecision(tabKnown, tabDisplayed, didCrash);
    }

    public boolean shouldRecreateWebView() {
        return mTabKnown;
    }

    public boolean shouldNotifyUser() {
        return mTabKnown && mTabDisplayed;
    }

    public boolean didCrash() {
        return mDidCrash;
    }
}
