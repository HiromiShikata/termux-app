package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OwnerCallBodyTable {

    @NonNull
    private final List<OwnerCallBodyRange> mHeaderCells;

    @NonNull
    private final List<OwnerCallBodyTableAlignment> mAlignments;

    @NonNull
    private final List<List<OwnerCallBodyRange>> mRows;

    public OwnerCallBodyTable(@NonNull List<OwnerCallBodyRange> headerCells,
                              @NonNull List<OwnerCallBodyTableAlignment> alignments,
                              @NonNull List<List<OwnerCallBodyRange>> rows) {
        mHeaderCells = Collections.unmodifiableList(new ArrayList<>(headerCells));
        mAlignments = Collections.unmodifiableList(new ArrayList<>(alignments));
        mRows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    @NonNull
    public List<OwnerCallBodyRange> getHeaderCells() {
        return mHeaderCells;
    }

    @NonNull
    public List<OwnerCallBodyTableAlignment> getAlignments() {
        return mAlignments;
    }

    @NonNull
    public List<List<OwnerCallBodyRange>> getRows() {
        return mRows;
    }
}
