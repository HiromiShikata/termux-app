package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

public enum DiagnosticsSessionListDisplay {

    DISPLAYED("displayed in list"),
    NOT_DISPLAYED("not displayed in list"),
    NOT_KNOWN("displayed in list unknown: the session list has not been built");

    @NonNull
    private final String mReportLabel;

    DiagnosticsSessionListDisplay(@NonNull String reportLabel) {
        mReportLabel = reportLabel;
    }

    @NonNull
    public String getReportLabel() {
        return mReportLabel;
    }

    @NonNull
    public static DiagnosticsSessionListDisplay ofSessionIndex(
            int sessionIndex, @Nullable Collection<Integer> sessionIndexesDisplayedInList) {
        if (sessionIndexesDisplayedInList == null) return NOT_KNOWN;
        return sessionIndexesDisplayedInList.contains(sessionIndex) ? DISPLAYED : NOT_DISPLAYED;
    }
}
