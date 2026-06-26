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

    @Nullable
    private final List<String> mAcknowledgedCallReasons;

    @Nullable
    private final Long mStatuslineCallTimeMillis;

    @Nullable
    private final Long mStatuslineOutTimeMillis;

    @Nullable
    private final Long mStatuslineReplyTimeMillis;

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis) {
        this(sessionName, lastOutputActivityTimeMillis, lastExplicitCallTimeMillis,
            lastExplicitCallReason, lastSeenTimeMillis, lastUserInputTimeMillis, null, null);
    }

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis,
                                   @Nullable List<String> unacknowledgedCallReasons) {
        this(sessionName, lastOutputActivityTimeMillis, lastExplicitCallTimeMillis,
            lastExplicitCallReason, lastSeenTimeMillis, lastUserInputTimeMillis,
            unacknowledgedCallReasons, null);
    }

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis,
                                   @Nullable List<String> unacknowledgedCallReasons,
                                   @Nullable List<String> acknowledgedCallReasons) {
        this(sessionName, lastOutputActivityTimeMillis, lastExplicitCallTimeMillis,
            lastExplicitCallReason, lastSeenTimeMillis, lastUserInputTimeMillis,
            unacknowledgedCallReasons, acknowledgedCallReasons, null, null, null);
    }

    public SessionNewActivityState(@NonNull String sessionName,
                                   @Nullable Long lastOutputActivityTimeMillis,
                                   @Nullable Long lastExplicitCallTimeMillis,
                                   @Nullable String lastExplicitCallReason,
                                   @Nullable Long lastSeenTimeMillis,
                                   @Nullable Long lastUserInputTimeMillis,
                                   @Nullable List<String> unacknowledgedCallReasons,
                                   @Nullable List<String> acknowledgedCallReasons,
                                   @Nullable Long statuslineCallTimeMillis,
                                   @Nullable Long statuslineOutTimeMillis,
                                   @Nullable Long statuslineReplyTimeMillis) {
        mSessionName = sessionName;
        mLastOutputActivityTimeMillis = lastOutputActivityTimeMillis;
        mLastExplicitCallTimeMillis = lastExplicitCallTimeMillis;
        mLastExplicitCallReason = lastExplicitCallReason;
        mLastSeenTimeMillis = lastSeenTimeMillis;
        mLastUserInputTimeMillis = lastUserInputTimeMillis;
        mUnacknowledgedCallReasons = unacknowledgedCallReasons == null
            ? null : Collections.unmodifiableList(new ArrayList<>(unacknowledgedCallReasons));
        mAcknowledgedCallReasons = acknowledgedCallReasons == null
            ? null : Collections.unmodifiableList(new ArrayList<>(acknowledgedCallReasons));
        mStatuslineCallTimeMillis = statuslineCallTimeMillis;
        mStatuslineOutTimeMillis = statuslineOutTimeMillis;
        mStatuslineReplyTimeMillis = statuslineReplyTimeMillis;
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

    @Nullable
    public List<String> getAcknowledgedCallReasons() {
        return mAcknowledgedCallReasons;
    }

    @Nullable
    public Long getStatuslineCallTimeMillis() {
        return mStatuslineCallTimeMillis;
    }

    @Nullable
    public Long getStatuslineOutTimeMillis() {
        return mStatuslineOutTimeMillis;
    }

    @Nullable
    public Long getStatuslineReplyTimeMillis() {
        return mStatuslineReplyTimeMillis;
    }
}
