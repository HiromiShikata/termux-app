package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Nullable
    private final Long mLastUserInputTimeMillis;

    @Nullable
    private final List<String> mUnacknowledgedCallReasons;

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis) {
        this(sessionName, lastOutputActivityTimeMillis, lastExplicitCallTimeMillis,
            lastExplicitCallReason, lastSeenTimeMillis, lastUserInputTimeMillis, null);
    }

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis,
                                   @Nullable List<String> unacknowledgedCallReasons) {
        mSessionName = sessionName;
        mLastOutputActivityTimeMillis = lastOutputActivityTimeMillis;
        mLastExplicitCallTimeMillis = lastExplicitCallTimeMillis;
        mLastExplicitCallReason = lastExplicitCallReason;
        mLastSeenTimeMillis = lastSeenTimeMillis;
        mLastUserInputTimeMillis = lastUserInputTimeMillis;
        mUnacknowledgedCallReasons = unacknowledgedCallReasons == null
            ? null : Collections.unmodifiableList(new ArrayList<>(unacknowledgedCallReasons));
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

    @Nullable
    public Long getLastUserInputTimeMillis() {
        return mLastUserInputTimeMillis;
    }

    @Nullable
    public List<String> getUnacknowledgedCallReasons() {
        return mUnacknowledgedCallReasons;
    }
}
