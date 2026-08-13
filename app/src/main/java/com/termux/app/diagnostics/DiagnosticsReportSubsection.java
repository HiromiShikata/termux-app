package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsReportSubsection {

    @NonNull
    private final String mName;

    private final int mLargestUsefulCharacters;

    public DiagnosticsReportSubsection(@NonNull String name, int largestUsefulCharacters) {
        mName = name;
        mLargestUsefulCharacters = largestUsefulCharacters;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public int getLargestUsefulCharacters() {
        return mLargestUsefulCharacters;
    }
}
