package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class SessionReconnectTimeoutOutcome {

    public enum Decision {
        NOT_RECONNECTING,
        RETRY,
        FAILED
    }

    @NonNull
    private final Decision mDecision;

    private final long mRetryBackoffMillis;

    private SessionReconnectTimeoutOutcome(@NonNull Decision decision, long retryBackoffMillis) {
        mDecision = decision;
        mRetryBackoffMillis = retryBackoffMillis;
    }

    @NonNull
    public static SessionReconnectTimeoutOutcome notReconnecting() {
        return new SessionReconnectTimeoutOutcome(Decision.NOT_RECONNECTING, 0L);
    }

    @NonNull
    public static SessionReconnectTimeoutOutcome retry(long retryBackoffMillis) {
        return new SessionReconnectTimeoutOutcome(Decision.RETRY, retryBackoffMillis);
    }

    @NonNull
    public static SessionReconnectTimeoutOutcome failed() {
        return new SessionReconnectTimeoutOutcome(Decision.FAILED, 0L);
    }

    @NonNull
    public Decision getDecision() {
        return mDecision;
    }

    public long getRetryBackoffMillis() {
        return mRetryBackoffMillis;
    }
}
