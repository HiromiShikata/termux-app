package com.termux.app.browser;

public final class BrowserBackPressDecision {

    public enum Result { NAVIGATE_BACK, KEEP_BROWSER_OPEN, SHOW_TERMINAL, NOT_HANDLED }

    private BrowserBackPressDecision() {}

    public static Result resolve(boolean browserVisible, boolean hasWebView, boolean canGoBack) {
        if (!browserVisible) return Result.NOT_HANDLED;
        if (!hasWebView) return Result.SHOW_TERMINAL;
        if (canGoBack) return Result.NAVIGATE_BACK;
        return Result.KEEP_BROWSER_OPEN;
    }
}
