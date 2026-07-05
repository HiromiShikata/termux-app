package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticEvent {

    private final long mTimestampMillis;
    @NonNull
    private final DiagnosticEventType mType;
    @NonNull
    private final String mDetail;

    public DiagnosticEvent(long timestampMillis, @NonNull DiagnosticEventType type, @NonNull String detail) {
        mTimestampMillis = timestampMillis;
        mType = type;
        mDetail = detail;
    }

    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    @NonNull
    public DiagnosticEventType getType() {
        return mType;
    }

    @NonNull
    public String getDetail() {
        return mDetail;
    }
}
