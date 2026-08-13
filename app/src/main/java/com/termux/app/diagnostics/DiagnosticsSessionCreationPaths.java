package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsSessionCreationPaths {

    public static final DiagnosticsSessionCreationPaths NONE =
        new DiagnosticsSessionCreationPaths(Collections.<DiagnosticsSessionCreationPathCount>emptyList());

    @NonNull
    private final List<DiagnosticsSessionCreationPathCount> mCountsByPath;

    public DiagnosticsSessionCreationPaths(@NonNull List<DiagnosticsSessionCreationPathCount> countsByPath) {
        mCountsByPath = Collections.unmodifiableList(countsByPath);
    }

    @NonNull
    public static DiagnosticsSessionCreationPaths of(@NonNull SessionCreationPathCounter counter) {
        List<DiagnosticsSessionCreationPathCount> countsByPath = new ArrayList<>();
        for (SessionCreationPath path : SessionCreationPath.values()) {
            int creationCount = counter.getCreationCount(path);
            if (creationCount > 0) {
                countsByPath.add(new DiagnosticsSessionCreationPathCount(path, creationCount));
            }
        }
        return new DiagnosticsSessionCreationPaths(countsByPath);
    }

    @NonNull
    public List<DiagnosticsSessionCreationPathCount> getCountsByPath() {
        return mCountsByPath;
    }

    public int getTotalCreationCount() {
        int total = 0;
        for (DiagnosticsSessionCreationPathCount countByPath : mCountsByPath) {
            total += countByPath.getCreationCount();
        }
        return total;
    }
}
