package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class SessionReconnectRetryPlan {

    private final long mTimeoutMillis;

    @NonNull
    private final long[] mRetryBackoffMillis;

    public SessionReconnectRetryPlan(long timeoutMillis, @NonNull long[] retryBackoffMillis) {
        mTimeoutMillis = timeoutMillis;
        mRetryBackoffMillis = retryBackoffMillis.clone();
    }

    public long getTimeoutMillis() {
        return mTimeoutMillis;
    }

    public int getMaxRetries() {
        return mRetryBackoffMillis.length;
    }

    @NonNull
    public SessionReconnectTimeoutOutcome onTimeout(@NonNull SessionNewActivityStore store,
                                                    @NonNull String sessionName) {
        if (!store.isReconnecting(sessionName)) {
            return SessionReconnectTimeoutOutcome.notReconnecting();
        }
        int completedAttempts = store.getReconnectRetryAttempt(sessionName);
        if (completedAttempts < mRetryBackoffMillis.length) {
            long backoffMillis = mRetryBackoffMillis[completedAttempts];
            store.incrementReconnectRetryAttempt(sessionName);
            return SessionReconnectTimeoutOutcome.retry(backoffMillis);
        }
        store.setReconnectFailed(sessionName);
        return SessionReconnectTimeoutOutcome.failed();
    }
}
