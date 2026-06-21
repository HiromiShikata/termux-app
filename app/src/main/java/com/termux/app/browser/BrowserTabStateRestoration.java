package com.termux.app.browser;

public final class BrowserTabStateRestoration {

    public enum Action {
        RESTORE,
        LOAD
    }

    private final Action mAction;

    private BrowserTabStateRestoration(Action action) {
        this.mAction = action;
    }

    public Action getAction() {
        return mAction;
    }

    public boolean shouldRestoreState() {
        return mAction == Action.RESTORE;
    }

    public boolean shouldLoadUrl() {
        return mAction == Action.LOAD;
    }

    public static BrowserTabStateRestoration resolve(boolean hasSavedState, boolean forceReload) {
        if (forceReload || !hasSavedState) {
            return new BrowserTabStateRestoration(Action.LOAD);
        }
        return new BrowserTabStateRestoration(Action.RESTORE);
    }
}
