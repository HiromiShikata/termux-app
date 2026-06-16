package com.termux.app.browser;

public final class ProjectBrowserSessionDismissal {

    private ProjectBrowserSessionDismissal() {
    }

    public static boolean shouldDismissOnSessionAccess(boolean overlayVisible) {
        return overlayVisible;
    }
}
