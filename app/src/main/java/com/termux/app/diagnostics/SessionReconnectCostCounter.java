package com.termux.app.diagnostics;

/**
 * Accumulates the main-thread time spent reconnecting dead sessions, together with how many sessions
 * were still waiting behind the slowest one, so a report can separate a single slow reconnect from a
 * burst that occupies the main thread once per session.
 * <p>
 * The stall watchdog only records a stall once the main thread has been blocked past its threshold,
 * so a run of reconnects that each stay under it leaves no trace there while the interface is
 * unresponsive for the whole run. This counter is what makes that run measurable.
 * <p>
 * A single {@link #record(long, int)} call performs no allocation, takes no lock and writes no log,
 * so an instance is cheap enough to stay enabled permanently on the main thread. State is
 * process-lifetime only and is never persisted.
 */
public final class SessionReconnectCostCounter {

    private static final long NANOS_PER_MILLISECOND = 1000000L;

    private long mReconnectCount;
    private long mTotalElapsedNanos;
    private long mMaxElapsedNanos;
    private int mSessionsStillQueuedAtMaxElapsed;

    public void record(long elapsedNanos, int sessionsStillQueued) {
        mReconnectCount++;
        mTotalElapsedNanos += elapsedNanos;
        if (elapsedNanos > mMaxElapsedNanos) {
            mMaxElapsedNanos = elapsedNanos;
            mSessionsStillQueuedAtMaxElapsed = sessionsStillQueued;
        }
    }

    public long getReconnectCount() {
        return mReconnectCount;
    }

    public long getTotalElapsedMillis() {
        return mTotalElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public long getMaxElapsedMillis() {
        return mMaxElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public int getSessionsStillQueuedAtMaxElapsed() {
        return mSessionsStillQueuedAtMaxElapsed;
    }
}
