package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionGroupingAutoRefresher {

    private final SessionDefinitionAutoReloadScheduler mScheduler;
    private final Runnable mRefreshGrouping;

    public SessionGroupingAutoRefresher(@NonNull Runnable refreshGrouping) {
        this(new SessionDefinitionAutoReloadScheduler(refreshGrouping), refreshGrouping);
    }

    SessionGroupingAutoRefresher(@NonNull SessionDefinitionAutoReloadScheduler scheduler,
        @NonNull Runnable refreshGrouping) {
        this.mScheduler = scheduler;
        this.mRefreshGrouping = refreshGrouping;
    }

    public void onForeground(@Nullable String definitionUrl) {
        SessionGroupingRefreshGate.refreshIfDefined(definitionUrl, mRefreshGrouping);
        mScheduler.onForeground(SessionGroupingRefreshGate.GROUPING_REFRESH_INTERVAL_MINUTES);
    }

    public void onBackground() {
        mScheduler.onBackground();
    }
}
