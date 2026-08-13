package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.scroll.TerminalScrollEvent;

public final class DiagnosticsScrollStepCount {

    @NonNull
    private final TerminalScrollEvent mDestination;

    private final int mStepCount;

    private final long mLastStepAtMillis;

    public DiagnosticsScrollStepCount(@NonNull TerminalScrollEvent destination, int stepCount,
                                      long lastStepAtMillis) {
        mDestination = destination;
        mStepCount = stepCount;
        mLastStepAtMillis = lastStepAtMillis;
    }

    @NonNull
    public String getDestinationLabel() {
        return DiagnosticsScrollDestinationLabel.of(mDestination);
    }

    public int getStepCount() {
        return mStepCount;
    }

    public long getLastStepAtMillis() {
        return mLastStepAtMillis;
    }
}
