package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class MainLooperQueuePeakRecorder {

    public interface ScrollbarViewCensusSource {

        @NonNull
        ScrollbarViewCensus takeScrollbarViewCensus();
    }

    @NonNull
    private DiagnosticsMainLooperQueuePeak mPeak = DiagnosticsMainLooperQueuePeak.NEVER_OBSERVED;

    public synchronized void recordObservation(@NonNull DiagnosticsMainLooperQueue mainLooperQueue,
                                               long observedAtMillis,
                                               @NonNull ScrollbarViewCensusSource scrollbarViewCensusSource) {
        if (mPeak.wasObserved() && mainLooperQueue.getPendingMessageCount() <= mPeak.getPendingMessageCount()) {
            return;
        }
        mPeak = DiagnosticsMainLooperQueuePeak.observedAt(mainLooperQueue.getPendingMessageCount(),
            observedAtMillis, mainLooperQueue.getBusiestTargets(),
            scrollbarViewCensusSource.takeScrollbarViewCensus());
    }

    @NonNull
    public synchronized DiagnosticsMainLooperQueuePeak snapshot() {
        return mPeak;
    }
}
