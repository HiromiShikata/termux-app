package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallBodySegment {

    private final int mStartIndex;
    private final int mEndIndex;

    @Nullable
    private final OwnerCallBodyTable mTable;

    private OwnerCallBodySegment(int startIndex, int endIndex, @Nullable OwnerCallBodyTable table) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mTable = table;
    }

    @NonNull
    public static OwnerCallBodySegment text(int startIndex, int endIndex) {
        return new OwnerCallBodySegment(startIndex, endIndex, null);
    }

    @NonNull
    public static OwnerCallBodySegment table(int startIndex, int endIndex,
                                             @NonNull OwnerCallBodyTable table) {
        return new OwnerCallBodySegment(startIndex, endIndex, table);
    }

    public int getStartIndex() {
        return mStartIndex;
    }

    public int getEndIndex() {
        return mEndIndex;
    }

    public boolean isTable() {
        return mTable != null;
    }

    @Nullable
    public OwnerCallBodyTable getTable() {
        return mTable;
    }
}
