package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public interface ProcessConditionSnapshotStore {

    @NonNull
    ProcessConditionSnapshot read();

    void write(@NonNull ProcessConditionSnapshot snapshot);
}
