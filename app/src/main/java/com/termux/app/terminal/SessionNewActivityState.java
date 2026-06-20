package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionNewActivityState {

    @NonNull
    private final String mSessionName;

    @Nullable
    private final Long mLastBellTimeMillis;

    @Nullable
    private final Long mLastSeenTimeMillis;

    public SessionNewActivityState(@NonNull String sessionName, @Nullable Long lastBellTimeMillis,
                                   @Nullable Long lastSeenTimeMillis) {
        mSessionName = sessionName;
        mLastBellTimeMillis = lastBellTimeMillis;
        mLastSeenTimeMillis = lastSeenTimeMillis;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
    }

    @Nullable
    public Long getLastBellTimeMillis() {
        return mLastBellTimeMillis;
    }

    @Nullable
    public Long getLastSeenTimeMillis() {
        return mLastSeenTimeMillis;
    }
}
