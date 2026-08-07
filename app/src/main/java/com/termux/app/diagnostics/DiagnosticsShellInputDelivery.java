package com.termux.app.diagnostics;

import androidx.annotation.Nullable;

public final class DiagnosticsShellInputDelivery {

    private final long mBytesAcceptedForDelivery;
    private final long mBytesWrittenToTheShell;
    private final long mBytesDiscardedBeforeDelivery;
    private final boolean mWriterRunning;

    @Nullable
    private final String mWriterStoppedReason;

    public DiagnosticsShellInputDelivery(long bytesAcceptedForDelivery, long bytesWrittenToTheShell,
                                         long bytesDiscardedBeforeDelivery, boolean writerRunning,
                                         @Nullable String writerStoppedReason) {
        mBytesAcceptedForDelivery = bytesAcceptedForDelivery;
        mBytesWrittenToTheShell = bytesWrittenToTheShell;
        mBytesDiscardedBeforeDelivery = bytesDiscardedBeforeDelivery;
        mWriterRunning = writerRunning;
        mWriterStoppedReason = writerStoppedReason;
    }

    public long getBytesAcceptedForDelivery() {
        return mBytesAcceptedForDelivery;
    }

    public long getBytesWrittenToTheShell() {
        return mBytesWrittenToTheShell;
    }

    public long getBytesDiscardedBeforeDelivery() {
        return mBytesDiscardedBeforeDelivery;
    }

    public long getBytesAcceptedButNotWrittenYet() {
        return Math.max(0L, mBytesAcceptedForDelivery - mBytesWrittenToTheShell);
    }

    public boolean isWriterRunning() {
        return mWriterRunning;
    }

    @Nullable
    public String getWriterStoppedReason() {
        return mWriterStoppedReason;
    }
}
