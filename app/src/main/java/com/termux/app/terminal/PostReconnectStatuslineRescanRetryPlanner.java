package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class PostReconnectStatuslineRescanRetryPlanner {

    @NonNull
    private final long[] mAbsoluteBackoffMillis;

    public PostReconnectStatuslineRescanRetryPlanner(@NonNull long[] absoluteBackoffMillis) {
        mAbsoluteBackoffMillis = absoluteBackoffMillis;
    }

    public long firstAttemptDelayMillis() {
        return mAbsoluteBackoffMillis[0];
    }

    public boolean hasAttemptAt(int attemptIndex) {
        return attemptIndex >= 0 && attemptIndex < mAbsoluteBackoffMillis.length;
    }

    public boolean shouldScheduleNextAttempt(int nextAttemptIndex, boolean allReconnectedSessionsParsed) {
        if (allReconnectedSessionsParsed) return false;
        return hasAttemptAt(nextAttemptIndex);
    }

    public long delayUntilNextAttemptMillis(int nextAttemptIndex) {
        if (!hasAttemptAt(nextAttemptIndex) || nextAttemptIndex == 0) {
            throw new IllegalArgumentException("no scheduled attempt at index " + nextAttemptIndex);
        }
        return mAbsoluteBackoffMillis[nextAttemptIndex] - mAbsoluteBackoffMillis[nextAttemptIndex - 1];
    }
}
