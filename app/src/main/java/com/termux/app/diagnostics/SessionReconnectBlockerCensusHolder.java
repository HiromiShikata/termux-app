package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionReconnectBlockerCensus;

public final class SessionReconnectBlockerCensusHolder {

    private static final SessionReconnectBlockerCensusHolder INSTANCE =
        new SessionReconnectBlockerCensusHolder();

    @NonNull
    private SessionReconnectBlockerCensus mCensus = SessionReconnectBlockerCensus.NOT_TAKEN;

    private SessionReconnectBlockerCensusHolder() {
    }

    @NonNull
    public static SessionReconnectBlockerCensusHolder getInstance() {
        return INSTANCE;
    }

    public synchronized void record(@NonNull SessionReconnectBlockerCensus census) {
        mCensus = census;
    }

    @NonNull
    public synchronized SessionReconnectBlockerCensus snapshot() {
        return mCensus;
    }
}
