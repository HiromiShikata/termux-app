package com.termux.app.process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface ProcessTable {

    @NonNull
    List<String> processIdentifiers();

    @Nullable
    String commandNameOf(@NonNull String processIdentifier);
}
