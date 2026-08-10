package com.termux.app.process;

import androidx.annotation.NonNull;

import com.termux.app.diagnostics.DiagnosticsAppProcessPopulation;
import com.termux.app.diagnostics.DiagnosticsProcessCommandCount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppProcessPopulationReader {

    @NonNull
    private final ProcessTable mProcessTable;

    public AppProcessPopulationReader() {
        this(new ProcFileSystemProcessTable());
    }

    public AppProcessPopulationReader(@NonNull ProcessTable processTable) {
        mProcessTable = processTable;
    }

    @NonNull
    public DiagnosticsAppProcessPopulation read() {
        List<String> processIdentifiers;
        try {
            processIdentifiers = mProcessTable.processIdentifiers();
        } catch (RuntimeException e) {
            return DiagnosticsAppProcessPopulation.readFailed(String.valueOf(e.getMessage()));
        }
        return DiagnosticsAppProcessPopulation.measured(
            processIdentifiers.size(), countsByCommandName(processIdentifiers));
    }

    @NonNull
    private List<DiagnosticsProcessCommandCount> countsByCommandName(
        @NonNull List<String> processIdentifiers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String processIdentifier : processIdentifiers) {
            String commandName = mProcessTable.commandNameOf(processIdentifier);
            String key = commandName == null
                ? DiagnosticsProcessCommandCount.UNREADABLE_COMMAND_NAME
                : commandName;
            Integer alreadyCounted = counts.get(key);
            counts.put(key, alreadyCounted == null ? 1 : alreadyCounted + 1);
        }
        return sortedByProcessCountDescending(counts);
    }

    @NonNull
    private List<DiagnosticsProcessCommandCount> sortedByProcessCountDescending(
        @NonNull Map<String, Integer> counts) {
        List<DiagnosticsProcessCommandCount> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            lines.add(new DiagnosticsProcessCommandCount(entry.getKey(), entry.getValue()));
        }
        Collections.sort(lines, new Comparator<DiagnosticsProcessCommandCount>() {
            @Override
            public int compare(DiagnosticsProcessCommandCount first,
                               DiagnosticsProcessCommandCount second) {
                if (first.getProcessCount() != second.getProcessCount()) {
                    return second.getProcessCount() - first.getProcessCount();
                }
                return first.getCommandName().compareTo(second.getCommandName());
            }
        });
        return lines;
    }
}
