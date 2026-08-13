package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogPaging {

    private final int index;
    private final int total;

    private OwnerCallDialogPaging(int index, int total) {
        this.index = index;
        this.total = total;
    }

    @NonNull
    public static OwnerCallDialogPaging resolve(int index, int total) {
        int clampedTotal = Math.max(0, total);
        if (clampedTotal == 0) {
            return new OwnerCallDialogPaging(0, 0);
        }
        return new OwnerCallDialogPaging(Math.min(Math.max(0, index), clampedTotal - 1), clampedTotal);
    }

    public int getIndex() {
        return index;
    }

    @NonNull
    public String getPositionLabel() {
        return (total == 0 ? 0 : index + 1) + " / " + total;
    }

    public boolean isPreviousEnabled() {
        return index > 0;
    }

    public boolean isNextEnabled() {
        return index + 1 < total;
    }
}
