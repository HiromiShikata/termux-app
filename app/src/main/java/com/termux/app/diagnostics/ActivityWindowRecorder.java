package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class ActivityWindowRecorder {

    private int mCreatedCount;

    private int mDestroyedCount;

    public synchronized void recordActivityCreated() {
        mCreatedCount++;
    }

    public synchronized void recordActivityDestroyed() {
        mDestroyedCount++;
    }

    @NonNull
    public synchronized DiagnosticsActivityWindows snapshot() {
        return new DiagnosticsActivityWindows(mCreatedCount, mDestroyedCount);
    }
}
