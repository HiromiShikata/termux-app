package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsMainLooperQueuePeak {

    @NonNull
    public static final DiagnosticsMainLooperQueuePeak NEVER_OBSERVED =
        new DiagnosticsMainLooperQueuePeak(false, 0, 0L, Collections.emptyList(),
            ScrollbarViewCensus.empty());

    private final boolean mObserved;

    private final int mPendingMessageCount;

    private final long mObservedAtMillis;

    @NonNull
    private final List<DiagnosticsMainLooperQueueTarget> mBusiestTargets;

    @NonNull
    private final ScrollbarViewCensus mScrollbarViewCensus;

    private DiagnosticsMainLooperQueuePeak(boolean observed, int pendingMessageCount, long observedAtMillis,
                                           @NonNull List<DiagnosticsMainLooperQueueTarget> busiestTargets,
                                           @NonNull ScrollbarViewCensus scrollbarViewCensus) {
        mObserved = observed;
        mPendingMessageCount = pendingMessageCount;
        mObservedAtMillis = observedAtMillis;
        mBusiestTargets = Collections.unmodifiableList(new ArrayList<>(busiestTargets));
        mScrollbarViewCensus = scrollbarViewCensus;
    }

    @NonNull
    public static DiagnosticsMainLooperQueuePeak observedAt(int pendingMessageCount, long observedAtMillis,
                                                            @NonNull List<DiagnosticsMainLooperQueueTarget> busiestTargets,
                                                            @NonNull ScrollbarViewCensus scrollbarViewCensus) {
        return new DiagnosticsMainLooperQueuePeak(true, pendingMessageCount, observedAtMillis, busiestTargets,
            scrollbarViewCensus);
    }

    public boolean wasObserved() {
        return mObserved;
    }

    public int getPendingMessageCount() {
        return mPendingMessageCount;
    }

    public long getObservedAtMillis() {
        return mObservedAtMillis;
    }

    @NonNull
    public List<DiagnosticsMainLooperQueueTarget> getBusiestTargets() {
        return mBusiestTargets;
    }

    @NonNull
    public ScrollbarViewCensus getScrollbarViewCensus() {
        return mScrollbarViewCensus;
    }
}
