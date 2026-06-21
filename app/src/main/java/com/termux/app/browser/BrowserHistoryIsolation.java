package com.termux.app.browser;

public final class BrowserHistoryIsolation {

    private final boolean mClearHistory;

    private BrowserHistoryIsolation(boolean clearHistory) {
        this.mClearHistory = clearHistory;
    }

    public boolean shouldClearHistory() {
        return mClearHistory;
    }

    public static BrowserHistoryIsolation resolve(boolean displayedTabChanged) {
        return new BrowserHistoryIsolation(displayedTabChanged);
    }
}
