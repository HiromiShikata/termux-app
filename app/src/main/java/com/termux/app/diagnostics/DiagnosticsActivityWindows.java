package com.termux.app.diagnostics;

public final class DiagnosticsActivityWindows {

    public static final DiagnosticsActivityWindows NONE = new DiagnosticsActivityWindows(0, 0);

    private final int mCreatedCount;

    private final int mDestroyedCount;

    public DiagnosticsActivityWindows(int createdCount, int destroyedCount) {
        mCreatedCount = createdCount;
        mDestroyedCount = destroyedCount;
    }

    public int getCreatedCount() {
        return mCreatedCount;
    }

    public int getDestroyedCount() {
        return mDestroyedCount;
    }

    public int getUndestroyedCount() {
        return mCreatedCount - mDestroyedCount;
    }
}
