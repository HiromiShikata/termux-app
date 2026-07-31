package com.termux.terminal;

/**
 * Accumulates the elapsed cost of a repeated piece of work whose duration grows with the number of
 * accumulated transcript rows, so a diagnostics report can attribute a progressive slowdown to a
 * specific mechanism without a profiler attached.
 * <p>
 * A single {@link #record(long, int)} call performs no allocation, takes no lock and writes no log,
 * so an instance is cheap enough to stay enabled permanently on the main thread. State is
 * process-lifetime only and is never persisted.
 */
public final class TranscriptWorkCostCounter {

    private static final long NANOS_PER_MILLISECOND = 1000000L;

    private long mSampleCount;
    private long mTotalElapsedNanos;
    private long mMaxElapsedNanos;
    private int mTranscriptRowsAtMaxElapsed;

    public void record(long elapsedNanos, int transcriptRows) {
        mSampleCount++;
        mTotalElapsedNanos += elapsedNanos;
        if (elapsedNanos > mMaxElapsedNanos) {
            mMaxElapsedNanos = elapsedNanos;
            mTranscriptRowsAtMaxElapsed = transcriptRows;
        }
    }

    public long getSampleCount() {
        return mSampleCount;
    }

    public long getTotalElapsedMillis() {
        return mTotalElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public long getMaxElapsedMillis() {
        return mMaxElapsedNanos / NANOS_PER_MILLISECOND;
    }

    public int getTranscriptRowsAtMaxElapsed() {
        return mTranscriptRowsAtMaxElapsed;
    }
}
