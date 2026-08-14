package com.termux.app.process;

import androidx.annotation.NonNull;

public final class ProcessThread {

    @NonNull
    private final String mIdentifier;

    @NonNull
    private final String mName;

    @NonNull
    private final String mSchedulerState;

    private final long mUserTimeTicks;

    private final long mSystemTimeTicks;

    public ProcessThread(@NonNull String identifier, @NonNull String name,
                         @NonNull String schedulerState, long userTimeTicks, long systemTimeTicks) {
        mIdentifier = identifier;
        mName = name;
        mSchedulerState = schedulerState;
        mUserTimeTicks = userTimeTicks;
        mSystemTimeTicks = systemTimeTicks;
    }

    @NonNull
    public String getIdentifier() {
        return mIdentifier;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public String getSchedulerState() {
        return mSchedulerState;
    }

    public long getUserTimeTicks() {
        return mUserTimeTicks;
    }

    public long getSystemTimeTicks() {
        return mSystemTimeTicks;
    }
}
