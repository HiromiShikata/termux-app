package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogPaging {

    private static final String POSITION_SEPARATOR = " / ";

    private final int mIndex;
    private final int mTotal;

    private OwnerCallDialogPaging(int index, int total) {
        mIndex = index;
        mTotal = total;
    }

    @NonNull
    public static OwnerCallDialogPaging resolve(int index, int total) {
        int waitingCallCount = Math.max(0, total);
        if (waitingCallCount == 0) {
            return new OwnerCallDialogPaging(0, 0);
        }
        return new OwnerCallDialogPaging(Math.min(Math.max(0, index), waitingCallCount - 1),
            waitingCallCount);
    }

    public int getIndex() {
        return mIndex;
    }

    @NonNull
    public String getPositionLabel() {
        return (mTotal == 0 ? 0 : mIndex + 1) + POSITION_SEPARATOR + mTotal;
    }

    public boolean isPreviousEnabled() {
        return mIndex > 0;
    }

    public boolean isNextEnabled() {
        return mIndex + 1 < mTotal;
    }
}
