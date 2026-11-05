package com.termux.app.ownercall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class OwnerCallBodySegment {

    private final int mStartIndex;
    private final int mEndIndex;

    @Nullable
    private final OwnerCallBodyTable mTable;

    private final boolean mQuoted;

    private OwnerCallBodySegment(int startIndex, int endIndex, @Nullable OwnerCallBodyTable table,
                                 boolean quoted) {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mTable = table;
        mQuoted = quoted;
    }

    @NonNull
    public static OwnerCallBodySegment text(int startIndex, int endIndex, boolean quoted) {
        return new OwnerCallBodySegment(startIndex, endIndex, null, quoted);
    }

    @NonNull
    public static OwnerCallBodySegment table(int startIndex, int endIndex,
                                             @NonNull OwnerCallBodyTable table, boolean quoted) {
        return new OwnerCallBodySegment(startIndex, endIndex, table, quoted);
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

    public boolean isQuoted() {
        return mQuoted;
    }
}
