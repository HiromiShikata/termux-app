package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsMainLooperQueueTarget {

    @NonNull
    private final String mDescription;

    private final int mPendingMessageCount;

    public DiagnosticsMainLooperQueueTarget(@NonNull String description, int pendingMessageCount) {
        mDescription = description;
        mPendingMessageCount = pendingMessageCount;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public int getPendingMessageCount() {
        return mPendingMessageCount;
    }
}
