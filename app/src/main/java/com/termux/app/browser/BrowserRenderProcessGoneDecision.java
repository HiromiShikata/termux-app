package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserRenderProcessGoneDecision {

    private final boolean mTabKnown;

    private final boolean mTabDisplayed;

    private final boolean mDidCrash;

    private final boolean mLooping;

    private BrowserRenderProcessGoneDecision(boolean tabKnown, boolean tabDisplayed, boolean didCrash, boolean looping) {
        this.mTabKnown = tabKnown;
        this.mTabDisplayed = tabDisplayed;
        this.mDidCrash = didCrash;
        this.mLooping = looping;
    }

    @NonNull
    public static BrowserRenderProcessGoneDecision forDiedWebView(boolean tabKnown, boolean tabDisplayed, boolean didCrash) {
        return new BrowserRenderProcessGoneDecision(tabKnown, tabDisplayed, didCrash, false);
    }

    @NonNull
    public static BrowserRenderProcessGoneDecision forDiedWebView(boolean tabKnown, boolean tabDisplayed, boolean didCrash,
            boolean looping) {
        return new BrowserRenderProcessGoneDecision(tabKnown, tabDisplayed, didCrash, looping);
    }

    public boolean shouldRecreateWebView() {
        return mTabKnown;
    }

    public boolean shouldReloadCrashedUrl() {
        return mTabKnown && !mLooping;
    }

    public boolean shouldLoadBlankPageInsteadOfReloading() {
        return mTabKnown && mLooping;
    }

    public boolean shouldNotifyUser() {
        return mTabKnown && mTabDisplayed;
    }

    public boolean didCrash() {
        return mDidCrash;
    }
}
