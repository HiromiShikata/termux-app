package com.termux.app.diagnostics;

public final class DiagnosticsShellExitCount {

    private final int mExitStatus;
    private final int mCount;

    public DiagnosticsShellExitCount(int exitStatus, int count) {
        mExitStatus = exitStatus;
        mCount = count;
    }

    public int getExitStatus() {
        return mExitStatus;
    }

    public int getCount() {
        return mCount;
    }
}
