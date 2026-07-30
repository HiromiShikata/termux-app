package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.terminal.TranscriptWorkCostCounter;

public final class DiagnosticsWorkCostLine {

    private final long mSampleCount;
    private final long mTotalElapsedMillis;
    private final long mMaxElapsedMillis;
    private final int mTranscriptRowsAtMaxElapsed;

    public DiagnosticsWorkCostLine(long sampleCount, long totalElapsedMillis, long maxElapsedMillis,
                                   int transcriptRowsAtMaxElapsed) {
        mSampleCount = sampleCount;
        mTotalElapsedMillis = totalElapsedMillis;
        mMaxElapsedMillis = maxElapsedMillis;
        mTranscriptRowsAtMaxElapsed = transcriptRowsAtMaxElapsed;
    }

    @NonNull
    public static DiagnosticsWorkCostLine of(@NonNull TranscriptWorkCostCounter counter) {
        return new DiagnosticsWorkCostLine(counter.getSampleCount(), counter.getTotalElapsedMillis(),
            counter.getMaxElapsedMillis(), counter.getTranscriptRowsAtMaxElapsed());
    }

    public long getSampleCount() {
        return mSampleCount;
    }

    public long getTotalElapsedMillis() {
        return mTotalElapsedMillis;
    }

    public long getMaxElapsedMillis() {
        return mMaxElapsedMillis;
    }

    public int getTranscriptRowsAtMaxElapsed() {
        return mTranscriptRowsAtMaxElapsed;
    }
}
