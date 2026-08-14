package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class DiagnosticsPreviousProcessExit {

    private final long mEndedAtMillis;

    private final int mReason;

    private final int mImportance;

    @Nullable
    private final String mDescription;

    public DiagnosticsPreviousProcessExit(long endedAtMillis, int reason, int importance,
                                          @Nullable String description) {
        mEndedAtMillis = endedAtMillis;
        mReason = reason;
        mImportance = importance;
        mDescription = description;
    }

    public long getEndedAtMillis() {
        return mEndedAtMillis;
    }

    @NonNull
    public String getReasonLabel() {
        return PreviousProcessExitReasonLabel.of(mReason);
    }

    @NonNull
    public String getImportanceLabel() {
        return PreviousProcessExitImportanceLabel.of(mImportance);
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }
}
