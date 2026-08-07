package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.List;

public final class DiagnosticsBackgroundCycle {

    private final long mCycleCount;

    @NonNull
    private final List<BackgroundCycleInterval> mLongestIntervals;

    public DiagnosticsBackgroundCycle(long cycleCount,
                                      @NonNull List<BackgroundCycleInterval> longestIntervals) {
        mCycleCount = cycleCount;
        mLongestIntervals = longestIntervals;
    }

    @NonNull
    public static DiagnosticsBackgroundCycle of(@NonNull BackgroundCycleIntervalRecorder recorder) {
        return new DiagnosticsBackgroundCycle(recorder.getCycleCount(), recorder.getLongestIntervals());
    }

    public long getCycleCount() {
        return mCycleCount;
    }

    @NonNull
    public List<BackgroundCycleInterval> getLongestIntervals() {
        return mLongestIntervals;
    }
}
