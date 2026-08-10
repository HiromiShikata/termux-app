package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class DiagnosticsAppProcessPopulation {

    public static final DiagnosticsAppProcessPopulation UNMEASURED =
        new DiagnosticsAppProcessPopulation(false, 0, Collections.emptyList(), null);

    private final boolean mWasMeasured;

    private final int mTotalProcessCount;

    @NonNull
    private final List<DiagnosticsProcessCommandCount> mCountsByCommandName;

    @Nullable
    private final String mReadFailureMessage;

    private DiagnosticsAppProcessPopulation(boolean wasMeasured, int totalProcessCount,
                                            @NonNull List<DiagnosticsProcessCommandCount> countsByCommandName,
                                            @Nullable String readFailureMessage) {
        mWasMeasured = wasMeasured;
        mTotalProcessCount = totalProcessCount;
        mCountsByCommandName = Collections.unmodifiableList(countsByCommandName);
        mReadFailureMessage = readFailureMessage;
    }

    @NonNull
    public static DiagnosticsAppProcessPopulation measured(
        int totalProcessCount, @NonNull List<DiagnosticsProcessCommandCount> countsByCommandName) {
        return new DiagnosticsAppProcessPopulation(true, totalProcessCount, countsByCommandName, null);
    }

    @NonNull
    public static DiagnosticsAppProcessPopulation readFailed(@NonNull String readFailureMessage) {
        return new DiagnosticsAppProcessPopulation(false, 0, Collections.emptyList(), readFailureMessage);
    }

    public boolean getWasMeasured() {
        return mWasMeasured;
    }

    public int getTotalProcessCount() {
        return mTotalProcessCount;
    }

    @NonNull
    public List<DiagnosticsProcessCommandCount> getCountsByCommandName() {
        return mCountsByCommandName;
    }

    @Nullable
    public String getReadFailureMessage() {
        return mReadFailureMessage;
    }
}
