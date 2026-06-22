package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionNewActivityState {

    @NonNull
    private final String mSessionName;

    @Nullable
    private final Long mLastOutputActivityTimeMillis;

    @Nullable
    private final Long mLastExplicitCallTimeMillis;

    @Nullable
    private final String mLastExplicitCallReason;

    @Nullable
    private final Long mLastSeenTimeMillis;

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis) {
        mSessionName = sessionName;
        mLastOutputActivityTimeMillis = lastOutputActivityTimeMillis;
        mLastExplicitCallTimeMillis = lastExplicitCallTimeMillis;
        mLastExplicitCallReason = lastExplicitCallReason;
        mLastSeenTimeMillis = lastSeenTimeMillis;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
    }

    @Nullable
    public Long getLastOutputActivityTimeMillis() {
        return mLastOutputActivityTimeMillis;
    }

    @Nullable
    public Long getLastExplicitCallTimeMillis() {
        return mLastExplicitCallTimeMillis;
    }

    @Nullable
    public String getLastExplicitCallReason() {
        return mLastExplicitCallReason;
    }

    @Nullable
    public Long getLastSeenTimeMillis() {
        return mLastSeenTimeMillis;
    }
}
