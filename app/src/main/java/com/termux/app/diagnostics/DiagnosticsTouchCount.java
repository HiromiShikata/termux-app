package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.touch.TerminalTouchKind;

public final class DiagnosticsTouchCount {

    @NonNull
    private final TerminalTouchKind mKind;

    private final int mTouchCount;

    private final long mLastTouchAtMillis;

    public DiagnosticsTouchCount(@NonNull TerminalTouchKind kind, int touchCount,
                                 long lastTouchAtMillis) {
        mKind = kind;
        mTouchCount = touchCount;
        mLastTouchAtMillis = lastTouchAtMillis;
    }

    @NonNull
    public String getKindLabel() {
        return DiagnosticsTouchKindLabel.of(mKind);
    }

    public int getTouchCount() {
        return mTouchCount;
    }

    public long getLastTouchAtMillis() {
        return mLastTouchAtMillis;
    }
}
