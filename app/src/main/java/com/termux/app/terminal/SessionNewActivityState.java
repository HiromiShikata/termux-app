package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionNewActivityState {

    @NonNull
    private final String mHandle;

    @Nullable
    private final Long mLastBellTimeMillis;

    @Nullable
    private final Long mLastSeenTimeMillis;

    public SessionNewActivityState(@NonNull String handle, @Nullable Long lastBellTimeMillis,
                                   @Nullable Long lastSeenTimeMillis) {
        mHandle = handle;
        mLastBellTimeMillis = lastBellTimeMillis;
        mLastSeenTimeMillis = lastSeenTimeMillis;
    }

    @NonNull
    public String getHandle() {
        return mHandle;
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
