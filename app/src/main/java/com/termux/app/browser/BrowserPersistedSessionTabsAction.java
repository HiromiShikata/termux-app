package com.termux.app.browser;

public enum BrowserPersistedSessionTabsAction {

    REWRITE_FROM_MEMORY,
    REMOVE,
    KEEP_PERSISTED;

    public static BrowserPersistedSessionTabsAction decide(boolean storedTabsWereLoadedForSession,
                                                           boolean sessionHasTabsInMemory) {
        return REWRITE_FROM_MEMORY;
    }
}
