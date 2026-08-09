package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public final class DiagnosticsShellExits {

    public static final DiagnosticsShellExits NONE =
        new DiagnosticsShellExits(Collections.<DiagnosticsShellExitCount>emptyList());

    @NonNull
    private final List<DiagnosticsShellExitCount> mCountsByExitStatus;

    public DiagnosticsShellExits(@NonNull List<DiagnosticsShellExitCount> countsByExitStatus) {
        mCountsByExitStatus = Collections.unmodifiableList(countsByExitStatus);
    }

    @NonNull
    public List<DiagnosticsShellExitCount> getCountsByExitStatus() {
        return mCountsByExitStatus;
    }

    public int getTotalExitCount() {
        int total = 0;
        for (DiagnosticsShellExitCount countByExitStatus : mCountsByExitStatus) {
            total += countByExitStatus.getCount();
        }
        return total;
    }
}
