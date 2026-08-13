package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.view.scroll.TerminalScrollEvent;
import com.termux.view.scroll.TerminalScrollStepCounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsScrollSteps {

    public static final DiagnosticsScrollSteps NONE =
        new DiagnosticsScrollSteps(Collections.<DiagnosticsScrollStepCount>emptyList());

    @NonNull
    private final List<DiagnosticsScrollStepCount> mCountsByDestination;

    public DiagnosticsScrollSteps(@NonNull List<DiagnosticsScrollStepCount> countsByDestination) {
        mCountsByDestination = Collections.unmodifiableList(countsByDestination);
    }

    @NonNull
    public static DiagnosticsScrollSteps of(@NonNull TerminalScrollStepCounter counter) {
        List<DiagnosticsScrollStepCount> countsByDestination = new ArrayList<>();
        for (TerminalScrollEvent destination : TerminalScrollEvent.values()) {
            int stepCount = counter.getStepCount(destination);
            if (stepCount > 0) {
                countsByDestination.add(new DiagnosticsScrollStepCount(destination, stepCount,
                    counter.getLastStepAtMillis(destination)));
            }
        }
        return new DiagnosticsScrollSteps(countsByDestination);
    }

    @NonNull
    public List<DiagnosticsScrollStepCount> getCountsByDestination() {
        return mCountsByDestination;
    }

    public int getTotalStepCount() {
        int totalStepCount = 0;
        for (DiagnosticsScrollStepCount countByDestination : mCountsByDestination) {
            totalStepCount += countByDestination.getStepCount();
        }
        return totalStepCount;
    }
}
