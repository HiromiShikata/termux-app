package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsSessionLine {

    @NonNull
    private final String mName;
    private final boolean mAlive;
    private final long mSecondsSinceLastActivity;
    private final boolean mHasLastActivity;
    private final int mTranscriptRows;
    private final int mColumns;

    @NonNull
    private final DiagnosticsShellInputDelivery mShellInputDelivery;

    public DiagnosticsSessionLine(@NonNull String name, boolean alive, long secondsSinceLastActivity,
                                  boolean hasLastActivity, int transcriptRows, int columns,
                                  @NonNull DiagnosticsShellInputDelivery shellInputDelivery) {
        mName = name;
        mAlive = alive;
        mSecondsSinceLastActivity = secondsSinceLastActivity;
        mHasLastActivity = hasLastActivity;
        mTranscriptRows = transcriptRows;
        mColumns = columns;
        mShellInputDelivery = shellInputDelivery;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public boolean isAlive() {
        return mAlive;
    }

    public long getSecondsSinceLastActivity() {
        return mSecondsSinceLastActivity;
    }

    public boolean hasLastActivity() {
        return mHasLastActivity;
    }

    public int getTranscriptRows() {
        return mTranscriptRows;
    }

    public int getColumns() {
        return mColumns;
    }

    @NonNull
    public DiagnosticsShellInputDelivery getShellInputDelivery() {
        return mShellInputDelivery;
    }
}
