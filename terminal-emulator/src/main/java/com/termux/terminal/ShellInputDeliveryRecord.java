package com.termux.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicLong;

public final class ShellInputDeliveryRecord {

    private final AtomicLong mBytesAcceptedForDelivery = new AtomicLong();

    private final AtomicLong mBytesWrittenToTheShell = new AtomicLong();

    private final AtomicLong mBytesDiscardedBeforeDelivery = new AtomicLong();

    private volatile boolean mWriterRunning;

    @Nullable
    private volatile String mWriterStoppedReason;

    @Nullable
    private volatile Long mLastBytesAcceptedAtMillis;

    @Nullable
    private volatile Long mLastBytesDiscardedAtMillis;

    public void recordWriterStarted() {
        mWriterStoppedReason = null;
        mWriterRunning = true;
    }

    public void recordWriterStopped(@NonNull String reason) {
        mWriterStoppedReason = reason;
        mWriterRunning = false;
    }

    public void recordBytesAcceptedForDelivery(int count, long acceptedAtMillis) {
        mBytesAcceptedForDelivery.addAndGet(count);
        mLastBytesAcceptedAtMillis = acceptedAtMillis;
    }

    public void recordBytesWrittenToTheShell(int count) {
        mBytesWrittenToTheShell.addAndGet(count);
    }

    public void recordBytesDiscardedBeforeDelivery(int count, long discardedAtMillis) {
        mBytesDiscardedBeforeDelivery.addAndGet(count);
        mLastBytesDiscardedAtMillis = discardedAtMillis;
    }

    public long getBytesAcceptedForDelivery() {
        return mBytesAcceptedForDelivery.get();
    }

    public long getBytesWrittenToTheShell() {
        return mBytesWrittenToTheShell.get();
    }

    public long getBytesDiscardedBeforeDelivery() {
        return mBytesDiscardedBeforeDelivery.get();
    }

    public long getBytesAcceptedButNotWrittenYet() {
        return Math.max(0L, getBytesAcceptedForDelivery() - getBytesWrittenToTheShell());
    }

    @Nullable
    public Long getLastBytesAcceptedAtMillis() {
        return mLastBytesAcceptedAtMillis;
    }

    @Nullable
    public Long getLastBytesDiscardedAtMillis() {
        return mLastBytesDiscardedAtMillis;
    }

    public boolean isWriterRunning() {
        return mWriterRunning;
    }

    @Nullable
    public String getWriterStoppedReason() {
        return mWriterStoppedReason;
    }
}
