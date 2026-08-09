package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ShellExitStatusRecorder {

    private final Map<Integer, Integer> mCountByExitStatus = new TreeMap<>();

    public synchronized void recordShellExit(int exitStatus) {
        Integer alreadyCounted = mCountByExitStatus.get(exitStatus);
        mCountByExitStatus.put(exitStatus, alreadyCounted == null ? 1 : alreadyCounted + 1);
    }

    @NonNull
    public synchronized DiagnosticsShellExits snapshot() {
        List<DiagnosticsShellExitCount> countsByExitStatus = new ArrayList<>();
        for (Map.Entry<Integer, Integer> countedExitStatus : mCountByExitStatus.entrySet()) {
            countsByExitStatus.add(new DiagnosticsShellExitCount(
                countedExitStatus.getKey(), countedExitStatus.getValue()));
        }
        Collections.sort(countsByExitStatus, new Comparator<DiagnosticsShellExitCount>() {
            @Override
            public int compare(DiagnosticsShellExitCount left, DiagnosticsShellExitCount right) {
                if (left.getCount() != right.getCount()) {
                    return right.getCount() - left.getCount();
                }
                return left.getExitStatus() - right.getExitStatus();
            }
        });
        return new DiagnosticsShellExits(countsByExitStatus);
    }
}
