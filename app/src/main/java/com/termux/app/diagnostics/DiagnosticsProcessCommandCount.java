package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsProcessCommandCount {

    public static final String UNREADABLE_COMMAND_NAME = "<unreadable>";

    @NonNull
    private final String mCommandName;

    private final int mProcessCount;

    public DiagnosticsProcessCommandCount(@NonNull String commandName, int processCount) {
        mCommandName = commandName;
        mProcessCount = processCount;
    }

    @NonNull
    public String getCommandName() {
        return mCommandName;
    }

    public int getProcessCount() {
        return mProcessCount;
    }
}
