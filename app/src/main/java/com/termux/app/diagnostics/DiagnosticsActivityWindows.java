package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsActivityWindows {

    public static final DiagnosticsActivityWindows NONE = new DiagnosticsActivityWindows(0, 0);

    private final int mCreatedCount;

    private final int mDestroyedCount;

    @NonNull
    private final DiagnosticsWindowCondition mCondition;

    public DiagnosticsActivityWindows(int createdCount, int destroyedCount) {
        this(createdCount, destroyedCount, DiagnosticsWindowCondition.UNMEASURED);
    }

    private DiagnosticsActivityWindows(int createdCount, int destroyedCount,
                                       @NonNull DiagnosticsWindowCondition condition) {
        mCreatedCount = createdCount;
        mDestroyedCount = destroyedCount;
        mCondition = condition;
    }

    @NonNull
    public DiagnosticsActivityWindows withCondition(@NonNull DiagnosticsWindowCondition condition) {
        return new DiagnosticsActivityWindows(mCreatedCount, mDestroyedCount, condition);
    }

    public int getCreatedCount() {
        return mCreatedCount;
    }

    public int getDestroyedCount() {
        return mDestroyedCount;
    }

    public int getTeardownNotRunCount() {
        return mCreatedCount - mDestroyedCount;
    }

    @NonNull
    public DiagnosticsWindowCondition getCondition() {
        return mCondition;
    }
}
