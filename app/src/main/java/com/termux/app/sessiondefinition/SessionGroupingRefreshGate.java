package com.termux.app.sessiondefinition;

import androidx.annotation.Nullable;

public final class SessionGroupingRefreshGate {

    public static final int GROUPING_REFRESH_INTERVAL_MINUTES = 1;

    private SessionGroupingRefreshGate() {
    }

    public static boolean shouldRefreshGrouping(@Nullable String definitionUrl) {
        return definitionUrl != null && !definitionUrl.trim().isEmpty();
    }
}
