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
    private final DiagnosticsSessionListDisplay mListDisplay;

    @NonNull
    private final DiagnosticsShellInputDelivery mShellInputDelivery;

    @NonNull
    private final DiagnosticsSessionStatusline mStatusline;

    @NonNull
    private final DiagnosticsScrollGestureRouting mScrollGestureRouting;

    public DiagnosticsSessionLine(@NonNull String name, boolean alive, long secondsSinceLastActivity,
                                  boolean hasLastActivity, int transcriptRows, int columns,
                                  @NonNull DiagnosticsSessionListDisplay listDisplay,
                                  @NonNull DiagnosticsShellInputDelivery shellInputDelivery,
                                  @NonNull DiagnosticsSessionStatusline statusline,
                                  @NonNull DiagnosticsScrollGestureRouting scrollGestureRouting) {
        mName = name;
        mAlive = alive;
        mSecondsSinceLastActivity = secondsSinceLastActivity;
        mHasLastActivity = hasLastActivity;
        mTranscriptRows = transcriptRows;
        mColumns = columns;
        mListDisplay = listDisplay;
        mShellInputDelivery = shellInputDelivery;
        mStatusline = statusline;
        mScrollGestureRouting = scrollGestureRouting;
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
    public DiagnosticsSessionListDisplay getListDisplay() {
        return mListDisplay;
    }

    @NonNull
    public DiagnosticsShellInputDelivery getShellInputDelivery() {
        return mShellInputDelivery;
    }

    @NonNull
    public DiagnosticsSessionStatusline getStatusline() {
        return mStatusline;
    }

    @NonNull
    public DiagnosticsScrollGestureRouting getScrollGestureRouting() {
        return mScrollGestureRouting;
    }
}
